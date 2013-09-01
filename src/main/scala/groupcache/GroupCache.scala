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

import collection.mutable.Map
import group.{GroupRegister, Group}
import peers.{NoPeerPicker, PeerPicker}
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.Future

class GroupCacheException(msg: String) extends Exception(msg)

/**
 * A group cache allows one or more groups of peers to participate in a
 * distributed cache.
 * @param peerPicker Determines which peer in the group owns a given key.
 *                   If omitted, the current process will act as the only peer.
 * @param serverStartHook Optional callback that is invoked when the first group is created.
 * @param newGroupHook Optional callback that is invoked each time a group is created.
 */
class GroupCache private(private val peerPicker: PeerPicker = NoPeerPicker,
                         private val serverStartHook: Option[() => Unit] = None,
                         private val newGroupHook: Option[(Group) => Unit] = None) extends GroupRegister {

  type Getter = (String, Option[Any]) => Future[ByteView]

  private val rwLock = new ReentrantReadWriteLock
  private val groups = Map[String, Group]()

  // Executes the server start callback when the first group is created.
  private lazy val runServerStartHook = {
    if (this.serverStartHook.isDefined) {
      this.serverStartHook.get()
    }
  }

  /**
   * Optionally gets the group with the given name.
   * @param name
   * @return
   */
  override def getGroup(name: String): Option[Group] = {
    rwLock.readLock.lock()
    try {
      groups.get(name)
    }
    finally {
      rwLock.readLock.unlock()
    }
  }

  /**
   * Adds a new group to start participating in a distributed cache.
   * @param name
   * @param maxCacheBytes The maximum number of total bytes that can be held
   *                      in both the main and hot caches of each peer of the group.
   * @param getter The non-blocking callback that is invoked when the current
   *               peer has been identified as the owner of a key, the corresponding
   *               value has not been cached, and the value needs to be fetched by
   *               the current peer (e.g., by retrieving the data from a database).
   * @return
   */
  def addGroup(name: String, maxCacheBytes: Long, getter: Getter): Group = {
    rwLock.writeLock.lock()
    try {
      runServerStartHook

      if (groups.contains(name)) {
        throw new GroupCacheException(s"A group with name '$name' already exists")
      }

      val group = new Group(name, getter, this.peerPicker, maxCacheBytes)

      if (this.newGroupHook.isDefined) {
        this.newGroupHook.get(group)
      }

      groups += name -> group
      group
    }
    finally {
      rwLock.writeLock.unlock()
    }
  }
}

/**
 * Companion object that ensures that only a single instance of GroupCache is created,
 * while allowing parameters to be passed to its constructor.
 */
object GroupCache {
  private val instanceCreated = new AtomicBoolean(false)

  /**
   * Constructs an instance of GroupCache if one does not already exist.
   * @param peerPicker
   * @param serverStartHook
   * @param newGroupHook
   * @return
   */
  def apply(peerPicker: PeerPicker,
            serverStartHook: Option[() => Unit] = None,
            newGroupHook: Option[(Group) => Unit] = None): GroupCache = {

    if (instanceCreated.compareAndSet(false, true)) {
       new GroupCache(peerPicker, serverStartHook, newGroupHook)
    }
    else {
      throw new GroupCacheException("Only one instance of group cache may be created")
    }
  }
}

