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

package groupcache.http

import groupcache.peers.{Peer, PeerPicker}
import java.util.zip.CRC32
import java.net.URL
import java.util.concurrent.locks.ReentrantLock

/**
 * Picks an HTTP peer as an owner of a given key
 * @param baseUrl Base URL of the current peer
 * @param peerUrls URL's of known HTTP peers
 */
class HttpPeerPicker(private val baseUrl: URL,
                     private var peerUrls: Array[URL]) extends PeerPicker {

  private val lock = new ReentrantLock()

  /**
   * Optionally picks an HTTP peer as the owner of the given key's value using
   * the checksum of the key.  Returns None if there are no peers or if the
   * peer with the given base URL is the owner
   */
  override def pickPeer(key: String): Option[Peer] = {
    val sum = checksum(key)
    var pickedPeer: Option[Peer] = None

    lock.lock()
    try {
      if (this.peerUrls.length > 0) {
        this.peerUrls(sum % this.peerUrls.length) match {
          case peerBaseUrl if peerBaseUrl != baseUrl => {
            pickedPeer = Some(new HttpPeer(peerBaseUrl))
          }
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
   * during the lifetime of this picker
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
   * Computes the checksum of the given key, which will be
   * used to determine which peer will "own" a given value
   */
  private def checksum(key: String): Int = {
    val crc = new CRC32
    val bytes = key.getBytes
    crc.update(bytes, 0, bytes.length)
    crc.getValue.toInt
  }
}

