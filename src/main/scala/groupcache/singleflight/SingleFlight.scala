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

package groupcache.singleflight

import collection.mutable.Map
import java.util.concurrent.locks.{ReentrantLock}
import scala.concurrent._
import ExecutionContext.Implicits.global

/**
 * Ensures that once a non-blocking function is invoked using a given key,
 * that function will not be invoked another time with that key until the
 * response from the first invocation is received.  If attempts are made
 * to execute the function while a request is enroute, a pending Future
 * object will be handed to the requester without ever invoking the function,
 * which can be used to determine when the value has become available.
 * @tparam Key
 * @tparam Value
 */
class SingleFlight[Key, Value] {
  private val lock = new ReentrantLock
  private val map = Map[Key, Future[Value]]()

  /**
   * Executes the given non-blocking function if there are no pending
   * requests using the given key.  Duplicate requests are returned
   * a pending Future without the function being invoked.
   * @param key
   * @param fn
   * @return
   */
  def execute(key: Key, fn: () => Future[Value]): Future[Value] = {
    lock.lock()
    val value = map.get(key)

    if (value.isDefined) {
      val ret = value.get
      lock.unlock()
      return ret
    }

    val promise = Promise[Value]()
    map += key -> promise.future
    lock.unlock()

    val f = fn()

    f onComplete {
      case _ => lock.lock()
                map -= key
                lock.unlock()
                promise.completeWith(f)
    }

    promise.future
  }
}

