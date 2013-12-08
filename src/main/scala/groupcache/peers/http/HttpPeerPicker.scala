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

import java.net.URL
import java.util.concurrent.locks.ReentrantLock
import groupcache.peers.{Peer, PeerPicker}
import groupcache.hash.RingHash
import groupcache.Implicits._

/**
 * Picks an HTTP peer as an owner of a given key.
 * @param baseUrl Base URL of the current peer.
 * @param peerUrls URL's of known HTTP peers.
 */
class HttpPeerPicker(private val baseUrl: URL,
                     peerUrls: Array[URL]) extends PeerPicker {

  private val lock = new ReentrantLock()
  private val defaultReplicas = 3
  private var peers = this.setPeers(peerUrls)

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

    lock.lock()
    try {
      if (!this.peers.isEmpty) {
        val potential = this.peers.get(key)

        if (potential != baseUrl.toString) {
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
  def setPeerUrls(peerUrls: Array[URL]): RingHash = {
    lock.lock()
    try {
      this.setPeers(peerUrls)
    }
    finally {
      lock.unlock()
    }
  }

  /**
   * Updates this instance's peers in cases where peers change dynamically
   * during the lifetime of this picker.
   */
  private def setPeers(peerUrls: Array[URL]): RingHash = {
    this.peers = new RingHash(this.defaultReplicas)
    this.peers.add(peerUrls.map(elem => elem.toString):_*)

    this.peers
  }
}

