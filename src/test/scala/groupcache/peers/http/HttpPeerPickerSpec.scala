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

import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers
import java.net.URL
import groupcache.Implicits._

class HttpPeerPickerSpec extends WordSpec with ShouldMatchers {
  "An HTTP peer picker" should {
    "Pick no peer when there are no peers to pick from" in {
      val picker = new HttpPeerPicker(80, Array[URL]())
      val peer = picker.pickPeer("key")
      peer should equal (None)
    }

    "Pick no peer when the current peer is the only one participating" in {
      val picker = new HttpPeerPicker(80, Array(new URL("http://localhost:80")))
      val peer = picker.pickPeer("key")
      peer should equal (None)
    }

    "Pick a peer when there are multiple peers and a key is not owned by the current peer" in {
      val picker = new HttpPeerPicker(80, Array(new URL("http://localhost:80"), new URL("http://peer:80")))
      val peer = picker.pickPeer("key")
      peer should not equal (None)
    }

    "Pick no peer when there are multiple peers and a key is owned by the current peer" in {
      val picker = new HttpPeerPicker("http://localhost:80", Array(new URL(new URL("http://peer:80"), "http://localhost:80")))
      val peer = picker.pickPeer("key")
      peer should equal (None)
    }

    "Allow its available peers to be updated" in {
      val picker = new HttpPeerPicker(80, Array[URL]())
      picker.setPeerUrls(Array(new URL("http://localhost:80"), new URL("http://peer:80")))
      val peer = picker.pickPeer("key")
      peer should not equal (None)
    }
  }
}

