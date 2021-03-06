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

import java.util.concurrent.atomic.AtomicInteger

/**
 * Tracks status of cache usage over a group.
 */
class GroupStats {
  /**
   * Number of gets that have been attempted, including attempts from peers.
   */
  val gets = new AtomicInteger(0)

  /**
   * Number of gets that have resulted in a cache hit.
   */
  val cacheHits = new AtomicInteger(0)

  /**
   * Number of remote loads or cache hits performed in the current process.
   */
  val peerLoads = new AtomicInteger(0)

  /**
   * Number of errors encountered when attempting to get a value from a peer.
   */
  val peerErrors = new AtomicInteger(0)

  /**
   * Number of gets that did not result in a cache hit.
   */
  val loads = new AtomicInteger(0)

  /**
   * Number of peer loads that resulted in an actual remote call.
   * Peer loads will be 'deduped' when concurrent requests for
   * the same key are made.
   */
  val loadsDeduped = new AtomicInteger(0)

  /**
   * Number of successful loads that were performed locally.
   */
  val localLoads = new AtomicInteger(0)

  /**
   * Number of unsuccessful loads that were performed locally.
   */
  val localLoadErrors = new AtomicInteger(0)

  /**
   * Number of gets that came over the network from peers.
   */
  val serverRequests = new AtomicInteger(0)
}

