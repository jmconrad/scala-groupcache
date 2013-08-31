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

/**
 * A peer picker maintains a set of peers that are part of a group
 * and determines which of those peers is the owner of a given key.
 * Returns None if the current peer is the owner of the key.
 */
trait PeerPicker {
  /**
   * Optionally picks a peer as the owner of the given key.  Returns
   * None if the current peer is the owner of the key.
   */
  def pickPeer(key: String) : Option[Peer]
}

