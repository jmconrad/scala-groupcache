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

import org.scalatest._
import matchers.ShouldMatchers
import peers.NoPeerPicker
import groupcache.group.Group
import groupcache.Implicits._
import scala.concurrent.Promise

/**
 * GroupCache is modeled as a singleton at the moment and isn't the easiest
 * to test.  These tests need to be run in order for them to succeed.
 */
class GroupCacheSpec extends WordSpec with ShouldMatchers {
  var serverStartInvocations = 0
  var newGroupInvocations = 0

  val serverStartHook: () => Unit = () => {
    serverStartInvocations += 1
  }

  val newGroupHook: (Group) => Unit = (group: Group) => {
    newGroupInvocations += 1
  }

  val groupCache = GroupCache(NoPeerPicker, Some(serverStartHook), Some(newGroupHook))
  val getter = (key: String, context: Option[Any]) => {
    val promise = Promise[ByteView]()
    promise.success("value")
    promise.future
  }

  "A group cache" should {
    "prevent multiple instances from being constructed" in {
      intercept[GroupCacheException] {
        GroupCache(NoPeerPicker)
      }
    }

    "track when the server has 'started' and when each group is created" in {
      groupCache.addGroup("group1", 1<<20, getter)
      groupCache.addGroup("group2", 1<<20, getter)

      serverStartInvocations should equal (1)
      newGroupInvocations should equal (2)
    }

    "disallow multiple groups with the same name" in {
      intercept[GroupCacheException] {
        groupCache.addGroup("group1", 1<<20, getter)
      }
    }

    "allow added groups to be retrieved" in {
      val group = groupCache.getGroup("group1")
      group should not equal (None)
    }

    "give a value of None when an invalid is attempted to be retrieved" in {
      val group = groupCache.getGroup("doesn't exist")
      group should equal (None)
    }
  }
}

