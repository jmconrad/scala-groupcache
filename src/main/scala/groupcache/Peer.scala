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

import groupcachepb.{GetRequest, GetResponse}

trait ProtoGetter {
  def get(context: Option[Any], in: GetRequest, out: GetResponse) : Unit
}

trait PeerPicker {
  def pickPeer(key: String) : (Option[ProtoGetter], Boolean)
}

object NoPeers extends PeerPicker {
  def pickPeer(key: String): (Option[ProtoGetter], Boolean) = {
    (None, false)
  }
}

class Peer(peerPickerFn: () => Option[PeerPicker] = () => None) {
  def getPeers: PeerPicker = {
    val picker = peerPickerFn()
    picker match {
      case Some(p: PeerPicker) => p
      case None => NoPeers
    }
  }
}

