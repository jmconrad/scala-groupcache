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
import org.scalatest._
import matchers.ShouldMatchers

class PeerSpec extends FlatSpec with ShouldMatchers {
  "A newly constructed peer" should "have no peers" in {
    new Peer().getPeers should equal (NoPeers)
  }

  "A peer with a registered picker fn that returns None" should "have no peers" in {
    val peer = new Peer(() => { None })
    peer.getPeers should equal (NoPeers)
  }

  "A peer with a registered picker fn that returns a non-None picker" should "have peers" in {
    object TestGetter extends ProtoGetter {
      def get(context: Option[Any], in: GetRequest, out: GetResponse) = {}
    }

    object TestPicker extends PeerPicker {
      def pickPeer(key: String): (Option[ProtoGetter], Boolean) = {
        (Some(TestGetter), true)
      }
    }

    val peer = new Peer(() => { Some(TestPicker) })
    peer.getPeers should equal (TestPicker)
  }
}

