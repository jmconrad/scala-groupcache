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

import java.util.concurrent.locks.ReentrantLock
import java.util.zip.CRC32

class HttpPoolPeerPicker(private val baseUrl: String,
                         private val basePath: String = "/_groupcache",
                         private val peers: Array[String]) extends PeerPicker {

  private val lock = new ReentrantLock

  def pickPeer(key: String): Option[Peer] = {
    val sum = checksum(key)
    var pickedPeer: Option[Peer] = None

    if (peers.length > 0) {
      lock.lock
      try {
        peers(sum % peers.length) match {
          case p if p != baseUrl => pickedPeer = Some(new HttpPeer(p, basePath))
        }
      }
      finally {
        lock.unlock
      }
    }

    pickedPeer
  }

  private def checksum(key: String): Int = {
    val crc = new CRC32
    val bytes = key.getBytes
    crc.update(bytes, 0, bytes.length)
    crc.getValue.toInt
  }
}

