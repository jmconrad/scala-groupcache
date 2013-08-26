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
import groupcache.sinks.Sink
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.util.Random
import com.google.protobuf.ByteString
import groupcache.peers.{Peer, PeerPicker}
import groupcache.lru.{CacheStats, SynchronizedCache}
import groupcache.singleflight.SingleFlight
import scala.Some
import groupcache.util.ByteView

class Group(val name: String,
            private val blockingGetter: (Option[Any], String, Sink) => Unit,
            private val peerPicker: PeerPicker,
            private val maxCacheBytes: Long, // Limit for sum of mainCache and hotCache size
            private val maxEntries: Int = 0) {

  private val mainCache = new SynchronizedCache(maxEntries)
  private val hotCache = new SynchronizedCache(maxEntries)
  private val stats = new GroupStats
  private val loadGroup = new SingleFlight[String, ByteView]

  def get(context: Option[Any], key: String, dest: Sink): Future[ByteView] = {
    stats.gets.incrementAndGet()
    val value = lookupCache(key)
    val promise = Promise[ByteView]()

    if (value.isDefined) {
      stats.cacheHits.incrementAndGet()
      setSinkView(dest, value.get)
      promise.success(value.get)
      return promise.future
    }

    val f = load(context, key, dest)

    f.map(result => {
      if (!result.destPopulated) {
        setSinkView(dest, result.value)
      }

      result.value
    })
  }

  import CacheType._
  def cacheStats(cacheType: CacheType): CacheStats = cacheType match {
    case HotCache => this.hotCache.stats
    case _ => this.mainCache.stats
  }

  private class LoadResult(val value: ByteView, val destPopulated: Boolean)

  private def load(context: Option[Any], key: String, dest: Sink): Future[LoadResult] = {
    this.stats.loads.incrementAndGet()
    var destPopulated = false

    val result = loadGroup.execute(key, () => {
      this.stats.loadsDeduped.incrementAndGet

      peerPicker.pickPeer(key) match {
        case None => {
          val localFuture = getLocally(context, key, dest)
          localFuture onFailure {
            case _ => this.stats.localLoadErrs.incrementAndGet()
          }

          localFuture.map(localVal => {
            this.stats.localLoads.incrementAndGet
             // Only one caller of load() gets this value.
            destPopulated = true
            this.populateCache(key, localVal, this.mainCache)
            localVal
          })
        }

        case Some(p: Peer) => {
          val peerFuture = getFromPeer(context, p, key)
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

    result.map(byteView => new LoadResult(byteView, destPopulated))
  }

  private def lookupCache(key: String): Option[ByteView] = {
    if (this.maxCacheBytes <= 0) {
      return None
    }

    mainCache.get(key) match {
      case value if value.isDefined => value
      case _ => hotCache.get(key)
    }
  }

  private def getLocally(context: Option[Any], key: String, dest: Sink): Future[ByteView] = {
    this.blockingGetter(context, key, dest)

    future {
      dest.view
    }
  }

  private def getFromPeer(context: Option[Any], peer: Peer, key: String): Future[ByteView] = {
    val request = new GetRequest(this.name, key)
    val f = peer.get(context, request)

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

  private def setSinkView(sink: Sink, view: ByteView): Unit = view.value match {
    case v if v.isLeft => sink.setBytes(v.left.get)
    case v => sink.setString(v.right.get)
  }
}

