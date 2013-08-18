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

import org.scalamock.scalatest.MockFactory
import org.scalatest._
import matchers.ShouldMatchers

class CacheSpec extends FlatSpec with ShouldMatchers with MockFactory {
  "An LRU cache" should "get a value for a key that has just been added" in {
    val cache = new Cache[String, String]
    cache.add("key", "value")
    cache.get("key") should equal (Some("value"))
  }

  "An LRU cache" should "update a value when a key already exists" in {
    val cache = new Cache[String, String]
    cache.add("key", "value")
    cache.add("key", "updatedValue")
    cache.get("key") should equal (Some("updatedValue"))
  }

  "An LRU cache" should "not get a value for a non-existent key" in {
    val cache = new Cache[String, String]
    cache.get("key") should equal (None)
  }

  "An LRU cache" should "evict the oldest entry when being explicitly instructed to do so" in {
    val cache = new Cache[String, String]
    cache.add("key", "value")
    cache.removeOldest
    cache.get("key") should equal (None)
  }

  "An LRU cache" should "evict the oldest entry when breaching its max size" in {
    val cache = new Cache[String, String](2)
    cache.add("key1", "value1")
    cache.add("key2", "value2")
    cache.add("key3", "value3")
    cache.get("key1") should equal (None)
    cache.get("key2") should equal (Some("value2"))
    cache.get("key3") should equal (Some("value3"))
  }

  "An LRU cache" should "not evict entries that have been recently accessed via get" in {
    val cache = new Cache[String, String](2)
    cache.add("key1", "value1")
    cache.add("key2", "value2")
    cache.get("key1")
    cache.add("key3", "value3")
    cache.get("key1") should equal (Some("value1"))
    cache.get("key2") should equal (None)
    cache.get("key3") should equal (Some("value3"))
  }

  "An LRU cache" should "not evict entries that have been recently accessed via add" in {
    val cache = new Cache[String, String](2)
    cache.add("key1", "value1")
    cache.add("key2", "value2")
    cache.add("key1", "updatedValue")
    cache.add("key3", "value3")
    cache.get("key1") should equal (Some("updatedValue"))
    cache.get("key2") should equal (None)
    cache.get("key3") should equal (Some("value3"))
  }

  "An LRU cache" should "allow an entry to be removed by key" in {
    val cache = new Cache[String, String]
    cache.add("key", "value")
    cache.remove("key")
    cache.get("key") should equal (None)
  }

  "An LRU cache" should "not invoke its onEvicted callback when no entry has been evicted" in {
    val fn = mockFunction[String, String, Unit]
    fn expects ("key", "value") never
    val cache = new Cache[String, String](1, Some(fn))
    cache.add("key", "value")
  }

  "An LRU cache" should "invoke its onEvicted callback when an entry has been evicted" in {
    val evictionFn = mockFunction[String, String, Unit]
    evictionFn expects ("key1", "value1") once
    val cache = new Cache[String, String](1, Some(evictionFn))
    cache.add("key1", "value1")
    cache.add("key2", "value2")
  }

  "An LRU cache" should "remove an entry by its key" in {
    val cache = new Cache[String, String]
    cache.add("key", "value")
    cache.remove("key")
    cache -= "key"
    cache.get("key") should equal (None)
  }
}

