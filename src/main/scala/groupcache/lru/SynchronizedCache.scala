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

import java.util.concurrent.locks.ReentrantReadWriteLock
import groupcache.ByteView

/**
 * Simple LRU cache that is safe for concurrent access and that
 * provides usage statistics.
 * @param maxEntries The maximum number of cache entries before an item is evicted.
 *                   Zero means no limit.
 */
class SynchronizedCache(private val maxEntries: Int = 0) {
  private val rwLock = new ReentrantReadWriteLock
  private var numEvictions = 0L
  private var numHits = 0L
  private var numGets = 0L

  // Total bytes of all keys and values.
  private var numBytes = 0L

  // Callback that is invoked when an entry is evicted.
  private val onEvicted = (key: String, value: ByteView) => {
    rwLock.writeLock.lock()
    try {
      numBytes -= (key.getBytes("UTF-8").length + value.length).toLong
      numEvictions += 1
    }
    finally {
      rwLock.writeLock.unlock()
    }
  }

  // Underlying thread-unsafe in-memory cache.
  private val lru = new Cache[String, ByteView](maxEntries, Some(onEvicted))

  /**
   * Adds a value to the cache.  If the key already exists, the entry's value
   * is updated.
   * @param key
   * @param value
   */
  def add(key: String, value: ByteView): Unit = {
    rwLock.writeLock.lock()
    try {
      lru.add(key, value)
      numBytes += (key.getBytes("UTF-8").length + value.length).toLong
    }
    finally {
      rwLock.writeLock.unlock()
    }
  }

  /**
   * Adds a value to the cache. If the key already exists, the entry's value
   * is updated.
   * @param key
   * @param value
   */
  def += (key: String, value: ByteView): Unit = add(key, value)

  /**
   * Optionally gets the key's value from the cache.
   * @param key
   * @return
   */
  def get(key: String): Option[ByteView] = {
    rwLock.writeLock.lock()
    try {
      numGets += 1
      val value = lru.get(key)

      if (value.isDefined) {
        numHits += 1
      }

      value
    }
    finally {
      rwLock.writeLock.unlock()
    }
  }

  /**
   * Removes the oldest entry from the cache.  Does nothing if the cache is empty.
   */
  def removeOldest: Unit = {
    rwLock.writeLock.lock()
    try {
      lru.removeOldest
    }
    finally {
      rwLock.writeLock.unlock()
    }
  }

  /**
   * Gets usage statistics.
   */
  def stats: CacheStats = {
    rwLock.readLock.lock()
    try {
      new CacheStats(
        bytes = numBytes,
        items = lru.itemCount,
        gets = numGets,
        hits = numHits,
        evictions = numEvictions
      )
    }
    finally {
      rwLock.readLock.unlock()
    }
  }
}

