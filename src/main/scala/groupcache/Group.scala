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

package groupcache

import _root_.groupcachepb.{GetResponse, GetRequest}
import groupcache.lru.SynchronizedCache
import peers.{Peer, NoPeerPicker, PeerPicker}
import groupcache.sinks.Sink
import singleflight.SingleFlight
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.util.{Random, Failure, Success}
import com.google.protobuf.ByteString

class Group(val name: String,
            private val blockingGetter: (Option[Any], String, Sink) => Unit,
            private val peers: PeerPicker,
            private val maxCacheBytes: Long, // Limit for sum of mainCache and hotCache size
            private val maxEntries: Int = 0) {

  private val mainCache = new SynchronizedCache(maxEntries)
  private val hotCache = new SynchronizedCache(maxEntries)
  private val stats = new GroupStats
  private val loadGroup = new SingleFlight[String, ByteView]

  def getPeerPicker(peerPickerFn: () => Option[PeerPicker] = () => None): PeerPicker = {
    val picker = peerPickerFn()
    picker match {
      case Some(p: PeerPicker) => p
      case _ => NoPeerPicker
    }
  }

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

    f onComplete {
      case Success(result) => {
        if (!result.destPopulated) {
          setSinkView(dest, result.value)
        }

        promise.success(result.value)
      }

      case Failure(t) => promise.failure(t)
    }

    promise.future
  }

  private class LoadResult(val value: ByteView, val destPopulated: Boolean) {}

  private def load(context: Option[Any], key: String, dest: Sink): Future[LoadResult] = {
    stats.loads.incrementAndGet()
    var destPopulated = false

    val result = loadGroup.execute(key, () => {
      stats.loadsDeduped.incrementAndGet

      peers.pickPeer(key) match {
        case None => {
          try {
            val localVal = getLocally(context, key, dest)
            stats.localLoads.incrementAndGet

            // Only one caller of load() gets this value.
            destPopulated = true
            populateCache(key, localVal, this.mainCache)
            localVal
          }
          catch {
            case e: Throwable => stats.localLoadErrs.incrementAndGet
                                 throw e
          }
        }

        case Some(p: Peer) => {
          try {
            val peerValue = getFromPeer(context, p, key)
            stats.peerLoads.incrementAndGet
            peerValue
          }
          catch {
            case e: Throwable => stats.peerErrors.incrementAndGet
                                 throw e
          }
        }
      }
    })

    val promise = Promise[LoadResult]()
    result onComplete {
      case Success(lr) => promise.success(new LoadResult(lr, destPopulated))
      case Failure(t) => promise.failure(t)
    }

    promise.future
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

  private def getLocally(context: Option[Any], key: String, dest: Sink): ByteView = {
    this.blockingGetter(context, key, dest)
    dest.view
  }

  private def getFromPeer(context: Option[Any], peer: Peer, key: String): ByteView = {
    val request = new GetRequest(this.name, key)
    val response = GetResponse.defaultInstance

    peer.get(context, request, response)

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

