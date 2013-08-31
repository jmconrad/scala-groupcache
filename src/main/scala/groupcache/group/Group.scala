/*
Copyright 2013 Josh Conrad

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package groupcache.group

import groupcachepb.GetRequest
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.util.Random
import com.google.protobuf.ByteString
import groupcache.peers.{Peer, PeerPicker}
import groupcache.lru.{CacheStats, SynchronizedCache}
import groupcache.singleflight.SingleFlight
import scala.Some
import groupcache.util.ByteView

/**
 * A group is a set of peers participating in a distributed cache.  Values
 * that are fetched from a group may be fetched locally (in process), or
 * may instead be fetched from a peer.
 *
 * A group holds two types of caches; a Main cache, which contains keys/values
 * for which the current peer (process) is the owner; and a Hot cache, which
 * contains keys/values for which the current peer (process) is not the owner,
 * but for which the keys are accessed frequently enough that the data needs
 * to be mirrored in this peer's cache.
 *
 * @param name The name of this group.
 * @param getter The non-blocking callback that is invoked when the current
 *               peer has been identified as the owner of a key, the corresponding
 *               value has not been cached, and the value needs to be fetched by
 *               the current peer (e.g., by retrieving the data from a database).
 * @param peerPicker Determines which peer in the group owns a given key.
 * @param maxCacheBytes  The maximum number of total bytes that can be held
 *                       in both the main and hot caches of each peer of the group.
 * @param maxEntries The maximum number of cache entries that can be held by
 *                   each type of cache.  Zero means no limit.
 */
class Group private[groupcache](
                    val name: String,
            private val getter: (String, Option[Any]) => Future[ByteView],
            private val peerPicker: PeerPicker,
            private val maxCacheBytes: Long,
            private val maxEntries: Int = 0) {

  private val mainCache = new SynchronizedCache(maxEntries)
  private val hotCache = new SynchronizedCache(maxEntries)
  private val stats = new GroupStats

  // Ensures that a key's value is not loaded multiple times due to
  // a rush of concurrent calls to fetch a particular key.
  private val loadGroup = new SingleFlight[String, ByteView]

  /**
   * Gets the value with the given key.  If the current peer is the owner of the key,
   * the value will be fetched locally.  Otherwise, the value will be fetched from
   * a peer.  If the key's value is not already loaded in the cache, this group's
   * non-blocking getter will be invoked to load it.
   * @param key
   * @param context Optional, opaque context data that will be passed to the
   *                non-blocking getter when invoked.
   * @return The Future value of the cache entry in the form of a byte view.
   */
  def get(key: String, context: Option[Any] = None): Future[ByteView] = {
    this.stats.gets.incrementAndGet()
    val promise = Promise[ByteView]()

    lookupCache(key) match {
      case Some(byteView: ByteView) => {
        this.stats.cacheHits.incrementAndGet()
        promise.success(byteView)
        promise.future
      }
      case _ => load(key, context)
    }
  }

  import CacheType._

  /**
   * Gets cache usage statistics aggregated across this group.
   * @param cacheType The type of cache of which to request statistics.
   */
  def cacheStats(cacheType: CacheType): CacheStats = cacheType match {
    case HotCache => this.hotCache.stats
    case _ => this.mainCache.stats
  }

  /**
   * Loads/fills the cache with a value using the given key.
   * @param key
   * @param context Optional, opaque context data that will be passed to
   *                the non-blocking getter if the value is loaded locally.
   * @return The Future value of the cache entry in the form of a byte view.
   */
  private def load(key: String, context: Option[Any]): Future[ByteView] = {
    this.stats.loads.incrementAndGet()

    val result = loadGroup.execute(key, () => {
      this.stats.loadsDeduped.incrementAndGet

      peerPicker.pickPeer(key) match {
        case None => {
          val localFuture = loadLocally(key, context)
          localFuture onFailure {
            case _ => this.stats.localLoadErrors.incrementAndGet()
          }

          localFuture.map(localVal => {
            this.stats.localLoads.incrementAndGet
            this.populateCache(key, localVal, this.mainCache)
            localVal
          })
        }

        case Some(p: Peer) => {
          val peerFuture = getFromPeer(p, key, context)
          peerFuture onFailure {
            case _ => this.stats.peerErrors.incrementAndGet()
          }

          peerFuture.map(peerValue => {
            this.stats.peerLoads.incrementAndGet()
            peerValue
          })
        }
      }
    })

    result
  }

  /**
   * Attempts to find the key's value in both the main and hot caches.
   * Returns None if not found in either.
   * @param key
   * @return
   */
  private def lookupCache(key: String): Option[ByteView] = {
    if (this.maxCacheBytes <= 0) {
      return None
    }

    mainCache.get(key) match {
      case value if value.isDefined => value
      case _ => hotCache.get(key)
    }
  }

  /**
   * Loads the value of the given key locally in this process.  This
   * should only be invoked once it is determined that the current peer
   * is the owner of the key.
   * @param key
   * @param context Optional, opaque context data that is passed to
   *                the non-blocking getter.
   * @return The Future value of the cache entry in the form of a byte view.
   */
  private def loadLocally(key: String, context: Option[Any]): Future[ByteView] = {
    this.getter(key, context)
  }

  /**
   * Gets or loads the value of the given key using the given peer.
   * @param peer
   * @param key
   * @param context
   * @return
   */
  private def getFromPeer(peer: Peer, key: String, context: Option[Any]): Future[ByteView] = {
    val request = new GetRequest(this.name, key)
    val f = peer.get(request, context)

    f.map(response => {
      val byteView = response.`value` match {
        case Some(byteString: ByteString) => ByteView(byteString.toByteArray)
        case _ => ByteView(Array[Byte]())
      }

      // Populate the local hot cache a percentage of the time a value is
      // fetched from a peer.  Should probably use something more logical
      // to determine if it needs to go into the hot cache.
      if (Random.nextInt(10) == 0) {
        populateCache(key, byteView, this.hotCache)
      }

      byteView
    })
  }

  /**
   * Adds a value to the given cache once it has been loaded either locally
   * or from a peer.
   * @param key
   * @param value
   * @param cache
   */
  private def populateCache(key: String, value: ByteView, cache: SynchronizedCache): Unit = {
    if (this.maxCacheBytes <= 0) {
      return
    }

    cache.add(key, value)

    while (true) {
      val mainBytes = this.mainCache.byteCount
      val hotBytes = this.hotCache.byteCount

      if (mainBytes + hotBytes <= this.maxCacheBytes) {
        return
      }

      var victim = mainCache
      if (hotBytes > mainBytes / 8) {
        victim = hotCache
      }

      victim.removeOldest
    }
  }
}

