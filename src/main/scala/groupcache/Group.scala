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

import groupcache.lru.SynchronizedCache
import peers.{NoPeerPicker, PeerPicker}
import groupcache.sinks.Sink

class Group(val name: String,
            private val getter: (Any, String, Sink) => Unit,
            private val peers: PeerPicker,
            private val maxEntries: Int = 0) {

  private val mainCache = new SynchronizedCache(maxEntries)
  private val hotCache = new SynchronizedCache(maxEntries)
  private val stats = new GroupStats

  def getPeerPicker(peerPickerFn: () => Option[PeerPicker] = () => None): PeerPicker = {
    val picker = peerPickerFn()
    picker match {
      case Some(p: PeerPicker) => p
      case _ => NoPeerPicker
    }
  }
}

