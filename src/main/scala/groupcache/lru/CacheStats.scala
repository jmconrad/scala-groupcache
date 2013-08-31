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

/**
 * Tracks stats of LRU cache usage.
 * @param bytes Total bytes of all keys and values.
 * @param items Number of key/value entries in the cache.
 * @param gets Number of gets that have been attempted.
 * @param hits Number of gets that have resulted in a cache hit.
 * @param evictions Number of entries that have been evicted
 *                  from the cache for any reason.
 */
class CacheStats(val bytes: Long,
                 val items: Long,
                 val gets: Long,
                 val hits: Long,
                 val evictions: Long) {
}

