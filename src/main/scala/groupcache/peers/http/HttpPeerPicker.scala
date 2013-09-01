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

package groupcache.peers.http

import java.util.zip.CRC32
import java.net.URL
import java.util.concurrent.locks.ReentrantLock
import groupcache.peers.{Peer, PeerPicker}

/**
 * Picks an HTTP peer as an owner of a given key.
 * @param baseUrl Base URL of the current peer.
 * @param peerUrls URL's of known HTTP peers.
 */
class HttpPeerPicker(private val baseUrl: URL,
                     private var peerUrls: Array[URL]) extends PeerPicker {

  private val lock = new ReentrantLock()

  /**
   * Constructs an HTTP peer picker using the given port.
   * @param localPort
   * @param peerUrls
   */
  def this(localPort: Int, peerUrls: Array[URL]) {
    this(new URL(s"http://localhost:$localPort"), peerUrls)
  }

  /**
   * Optionally picks an HTTP peer as the owner of the given key's value using
   * the checksum of the key.  Returns None if there are no peers or if the
   * peer with the given base URL is the owner.
   */
  override def pickPeer(key: String): Option[Peer] = {
    var pickedPeer: Option[Peer] = None
    var sum = checksum(key).toInt

    if (sum < 0) {
      sum *= -1
    }

    lock.lock()
    try {
      if (this.peerUrls.length > 0) {
        val potential = this.peerUrls(sum % this.peerUrls.length)

        if (potential != baseUrl) {
          pickedPeer = Some(new HttpPeer(potential))
        }
      }
    }
    finally {
      lock.unlock()
    }

    pickedPeer
  }

  /**
   * Updates this instance's peers in cases where peers change dynamically
   * during the lifetime of this picker.
   */
  def setPeerUrls(peerUrls: Array[URL]): Unit = {
    lock.lock()
    try {
      this.peerUrls = peerUrls
    }
    finally {
      lock.unlock()
    }
  }

  /**
   * Computes the checksum of the given key, which will be.
   * used to determine which peer will "own" a given value
   */
  private def checksum(key: String): Long = {
    val crc = new CRC32
    val bytes = key.getBytes
    crc.update(bytes, 0, bytes.length)
    crc.getValue
  }
}

