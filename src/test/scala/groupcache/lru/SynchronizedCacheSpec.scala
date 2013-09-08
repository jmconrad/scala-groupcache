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

package groupcache.lru

import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers
import groupcache.ByteView
import groupcache.Implicits._

class SynchronizedCacheSpec extends WordSpec with ShouldMatchers {
  "A synchronized LRU cache" should {
    "get a value for a key that has just been added" in {
      val cache = new SynchronizedCache
      cache.add("key", "value")
      assert(cache.get("key").get.sameElements(ByteView("value")))
    }

    "update a value when a key already exists" in {
      val cache = new SynchronizedCache
      cache.add("key", "value")
      cache.add("key", "updatedValue")
      assert(cache.get("key").get.sameElements(ByteView("updatedValue")))
    }

    "not get a value for a non-existent key" in {
      val cache = new SynchronizedCache
      cache.get("key") should equal (None)
    }

    "evict the oldest entry when being explicitly instructed to do so" in {
      val cache = new SynchronizedCache
      cache.add("key", "value")
      cache.removeOldest
      cache.get("key") should equal (None)
    }

    "evict the oldest entry when breaching its max size" in {
      val cache = new SynchronizedCache(2)
      cache.add("key1", "value1")
      cache.add("key2", "value2")
      cache.add("key3", "value3")
      cache.get("key1") should equal (None)
      assert(cache.get("key2").get.sameElements(ByteView("value2")))
      assert(cache.get("key3").get.sameElements(ByteView("value3")))
    }

    "not evict entries that have been recently accessed via get" in {
      val cache = new SynchronizedCache(2)
      cache.add("key1", "value1")
      cache.add("key2", "value2")
      cache.get("key1")
      cache.add("key3", "value3")
      assert(cache.get("key1").get.sameElements(ByteView("value1")))
      cache.get("key2") should equal (None)
      assert(cache.get("key3").get.sameElements(ByteView("value3")))
    }

    "not evict entries that have been recently accessed via add" in {
      val cache = new SynchronizedCache(2)
      cache.add("key1", "value1")
      cache.add("key2", "value2")
      cache.add("key1", "updatedValue")
      cache.add("key3", "value3")
      assert(cache.get("key1").get.sameElements(ByteView("updatedValue")))
      cache.get("key2") should equal (None)
      assert(cache.get("key3").get.sameElements(ByteView("value3")))
    }

    "track its byte count" in {
      val cache = new SynchronizedCache
      val key = "key"
      val value = "value"
      cache.add(key, value)
      cache.stats.bytes should equal (key.getBytes("UTF-8").length + value.getBytes("UTF-8").length)
    }

    "track its item count" in {
      val cache = new SynchronizedCache(2)
      cache.add("key1", "value1")
      cache.add("key2", "value2")
      cache.add("key3", "value3")
      cache.stats.items should equal (2)
    }

    "track its number of evictions" in {
      val cache = new SynchronizedCache(1)
      cache.add("key1", "value1")
      cache.add("key2", "value2")
      cache.add("key3", "value3")
      cache.stats.evictions should equal (2)
    }

    "track its get and hit counts" in {
      val cache = new SynchronizedCache
      cache.add("key", "value")
      cache.get("key")
      cache.get("key")
      cache.get("key2")
      cache.stats.gets should equal (3)
      cache.stats.hits should equal (2)
    }
  }
}

