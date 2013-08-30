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

package groupcache.peers

import concurrent.Future
import groupcachepb.{GetRequest, GetResponse}

/**
 * A peer is a part of a group of other peers that can receive asynchronous
 * requests for protobuf-encoded cached values.  A peer can participate
 * in multiple groups
 */
trait Peer {
  /**
   * Asynchronously gets a cached value from this peer
   * @param request protobuf-encoded request containing group name and key
   * @param context optional context data
   * @return a future protobuf-encoded response
   */
  def get(request: GetRequest, context: Option[Any] = None): Future[GetResponse]
}

