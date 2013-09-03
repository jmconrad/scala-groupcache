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

import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers
import concurrent.{Future, Await, Promise}
import concurrent.duration._
import groupcache.ByteView
import groupcache.Implicits._
import groupcache.peers.{Peer, PeerPicker, NoPeerPicker}
import org.scalamock.scalatest.MockFactory
import groupcachepb.{GetResponse, GetRequest}
import com.google.protobuf.ByteString
import CacheType._

class GroupSpec extends WordSpec with ShouldMatchers with MockFactory {
  val getter = (key: String, context: Option[Any]) => {
    val promise = Promise[ByteView]()
    promise.success("value")
    promise.future
  }

  val failedGetter = (key: String, context: Option[Any]) => {
    val promise = Promise[ByteView]()
    promise.failure(new Exception("fail"))
    promise.future
  }

  "A group with no peers" should {
    "load a cache value using its provided getter when a key is not in the cache" in {
      val group = new Group("test group", getter, NoPeerPicker, 1<<20)
      val future = group.get("key")
      val result = Await.result(future, 500 millis)

      result should equal (ByteView("value"))

      group.groupStats.cacheHits.get should equal (0)
      group.groupStats.loads.get should equal (1)
      group.groupStats.gets.get should equal (1)
      group.groupStats.loadsDeduped.get should equal (1)
      group.groupStats.localLoadErrors.get should equal (0)
      group.groupStats.localLoads.get should equal (1)
      group.groupStats.peerErrors.get should equal (0)
      group.groupStats.peerLoads.get should equal (0)

      val mainCacheStats = group.cacheStats(MainCache)
      mainCacheStats.bytes should equal (("key".getBytes("UTF-8").length + "value".getBytes("UTF-8").length).toLong)
      mainCacheStats.evictions should equal (0)
      mainCacheStats.gets should equal (1)
      mainCacheStats.hits should equal (0)
      mainCacheStats.items should equal (1)
    }

    "not invoke its getter when its value is already in the cache" in {
      val getter = mockFunction[String, Option[Any], Future[ByteView]]
      val promise = Promise[ByteView]()
      promise.success("value")

      getter expects ("key", None) returning promise.future once

      val group = new Group("test group", getter, NoPeerPicker, 1 << 20)
      Await.result(group.get("key"), 500 millis)
      Await.result(group.get("key"), 500 millis)

      group.groupStats.cacheHits.get should equal (1)
      group.groupStats.loads.get should equal (1)
      group.groupStats.gets.get should equal (2)
      group.groupStats.loadsDeduped.get should equal (1)
      group.groupStats.localLoadErrors.get should equal (0)
      group.groupStats.localLoads.get should equal (1)
      group.groupStats.peerErrors.get should equal (0)
      group.groupStats.peerLoads.get should equal (0)

      val mainCacheStats = group.cacheStats(MainCache)
      mainCacheStats.bytes should equal (("key".getBytes("UTF-8").length + "value".getBytes("UTF-8").length).toLong)
      mainCacheStats.evictions should equal (0)
      mainCacheStats.gets should equal (2)
      mainCacheStats.hits should equal (1)
      mainCacheStats.items should equal (1)
    }

    "not cache an item if the max cache bytes value has been surpassed" in {
      val getter = mockFunction[String, Option[Any], Future[ByteView]]
      val promise = Promise[ByteView]()
      promise.success("value")

      getter expects ("key", None) returning promise.future twice

      val group = new Group("test group", getter, NoPeerPicker, 0)
      Await.result(group.get("key"), 500 millis)
      Await.result(group.get("key"), 500 millis)

      group.groupStats.cacheHits.get should equal (0)
      group.groupStats.loads.get should equal (2)
      group.groupStats.gets.get should equal (2)
      group.groupStats.loadsDeduped.get should equal (2)
      group.groupStats.localLoadErrors.get should equal (0)
      group.groupStats.localLoads.get should equal (2)
      group.groupStats.peerErrors.get should equal (0)
      group.groupStats.peerLoads.get should equal (0)

      val mainCacheStats = group.cacheStats(MainCache)
      mainCacheStats.bytes should equal (0)
      mainCacheStats.evictions should equal (0)
      mainCacheStats.gets should equal (0)
      mainCacheStats.hits should equal (0)
      mainCacheStats.items should equal (0)
    }

    "maintain the correct stats when a local load fails" in {
      val group = new Group("test group", failedGetter, NoPeerPicker, 1<<20)
      Await.ready(group.get("key"), 500 millis)

      group.groupStats.cacheHits.get should equal (0)
      group.groupStats.loads.get should equal (1)
      group.groupStats.gets.get should equal (1)
      group.groupStats.loadsDeduped.get should equal (1)
      group.groupStats.localLoadErrors.get should equal (1)
      group.groupStats.localLoads.get should equal (0)
      group.groupStats.peerErrors.get should equal (0)
      group.groupStats.peerLoads.get should equal (0)

      val mainCacheStats = group.cacheStats(MainCache)
      mainCacheStats.bytes should equal (0)
      mainCacheStats.evictions should equal (0)
      mainCacheStats.gets should equal (1)
      mainCacheStats.hits should equal (0)
      mainCacheStats.items should equal (0)
    }
  }


  "A group with peers" should {
    "load a cache value from a peer when getting a key not owned by the current peer" in {
      var wasGetCalled = false

      class MockPeer extends Peer {
        def get(request: GetRequest, context: Option[Any]): Future[GetResponse] = {
          wasGetCalled = true
          val promise = Promise[GetResponse]()
          val response = new GetResponse(Some(ByteString.copyFrom("value".getBytes("UTF-8"))))
          promise.success(response)
          promise.future
        }
      }

      class MockPeerPicker extends PeerPicker {
        def pickPeer(key: String): Option[Peer] = {
          Some(new MockPeer)
        }
      }

      val group = new Group("test group", getter, new MockPeerPicker, 1<<20)
      val future = group.get("key")
      val result = Await.result(future, 500 millis)

      wasGetCalled should equal (true)
      result should equal (ByteView("value"))

      group.groupStats.cacheHits.get should equal (0)
      group.groupStats.loads.get should equal (1)
      group.groupStats.gets.get should equal (1)
      group.groupStats.loadsDeduped.get should equal (1)
      group.groupStats.localLoadErrors.get should equal (0)
      group.groupStats.localLoads.get should equal (0)
      group.groupStats.peerErrors.get should equal (0)
      group.groupStats.peerLoads.get should equal (1)

      val mainCacheStats = group.cacheStats(MainCache)
      mainCacheStats.bytes should equal (0)
      mainCacheStats.evictions should equal (0)
      mainCacheStats.gets should equal (1)
      mainCacheStats.hits should equal (0)
      mainCacheStats.items should equal (0)
    }

    "maintain the correct stats when a peer load fails" in {
      class MockPeer extends Peer {
        def get(request: GetRequest, context: Option[Any]): Future[GetResponse] = {
          val promise = Promise[GetResponse]()
          promise.failure(new Exception("fail"))
          promise.future
        }
      }

      class MockPeerPicker extends PeerPicker {
        def pickPeer(key: String): Option[Peer] = {
          Some(new MockPeer)
        }
      }

      val group = new Group("test group", failedGetter, new MockPeerPicker, 1<<20)
      Await.ready(group.get("key"), 500 millis)

      group.groupStats.cacheHits.get should equal (0)
      group.groupStats.loads.get should equal (1)
      group.groupStats.gets.get should equal (1)
      group.groupStats.loadsDeduped.get should equal (1)
      group.groupStats.localLoadErrors.get should equal (0)
      group.groupStats.localLoads.get should equal (0)
      group.groupStats.peerErrors.get should equal (1)
      group.groupStats.peerLoads.get should equal (0)

      val mainCacheStats = group.cacheStats(MainCache)
      mainCacheStats.bytes should equal (0)
      mainCacheStats.evictions should equal (0)
      mainCacheStats.gets should equal (1)
      mainCacheStats.hits should equal (0)
      mainCacheStats.items should equal (0)
    }
  }
}

