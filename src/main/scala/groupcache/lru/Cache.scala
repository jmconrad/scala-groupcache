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

import collection.mutable.{ListBuffer, Map}

/**
 * A simple LRU cache that is not safe for concurrent access.
 *
 * @param maxEntries The maximum number of cache entries before an item is evicted.
 *                   Zero means no limit.
 *
 * @param onEvicted  Optional callback function that takes key/value parameters,
 *                   to be executed when an entry is purged from the cache.
 */
class Cache[Key, Value](private val maxEntries: Int = 0,
                        private val onEvicted: Option[(Key, Value) => Unit] = None) {

  private class CacheEntry(val key: Key, var value: Value) {}
  private val lruTracker = ListBuffer[CacheEntry]()
  private val internalCache = Map[Key, CacheEntry]()

  /**
   * Adds a value to the cache. If the key already exists, the entry's value
   * is updated.
   * @param key
   * @param value
   */
  def add(key: Key, value: Value): Unit = internalCache.get(key) match {
    case Some(entry) => lruTracker -= entry
                        lruTracker.prepend(entry)
                        entry.value = value

    case None => val entry = new CacheEntry(key, value)
                 lruTracker.prepend(entry)
                 internalCache(key) = entry
                 if (maxEntries != 0 && lruTracker.length > maxEntries) {
                   removeOldest
                 }
  }

  /**
   * Adds a value to the cache. If the key already exists, the entry's value
   * is updated.
   * @param key
   * @param value
   */
  def += (key: Key, value: Value): Unit = add(key, value)

  /**
   * Optionally gets key's value from the cache.
   * @param key
   * @return
   */
  def get(key: Key): Option[Value] = internalCache.get(key) match {
    case Some(entry) => lruTracker -= entry
                        lruTracker.prepend(entry)
                        Some(entry.value)

    case None => None
  }

  /**
   * Removes the entry with the given key from the cache.  Does nothing
   * if the key does not exist in the cache.
   * @param key
   */
  def remove(key: Key): Unit = {
    val value = internalCache.get(key)

    if (value.isDefined) {
      removeElement(value.get)
    }
  }

  /**
   * Removes the entry with the given key from the cache.  Does nothing
   * if the key does not exist in the cache.
   * @param key
   */
  def -= (key: Key): Unit = remove(key)

  /**
   * Removes the oldest entry from the cache.  Does nothing if the cache is empty.
   */
  def removeOldest: Unit = {
    if (lruTracker.length > 0) {
      removeElement(lruTracker.last)
    }
  }

  /**
   * Gets the number of entries currently held in the cache.
   */
  def itemCount: Int = {
    lruTracker.length
  }

  /**
   * Removes the given entry from the cache.  Invokes the onEvicted() function
   * if defined.
   * @param entry
   */
  private def removeElement(entry: CacheEntry): Unit = {
    lruTracker -= entry
    internalCache -= entry.key

    if (onEvicted.isDefined) {
      onEvicted.get(entry.key, entry.value)
    }
  }
}

