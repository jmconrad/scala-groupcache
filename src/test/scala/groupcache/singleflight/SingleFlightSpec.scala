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

import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalamock.scalatest.MockFactory
import concurrent.{Await, Promise, Future}
import concurrent.duration._

class SingleFlightSpec extends WordSpec with ShouldMatchers with MockFactory {
  "A single flight" should {
    "not invoke a function while another invocation of it is still in progress" in {
      val singleFlight = new SingleFlight[String, String]
      val promise = Promise[String]()
      val fn = mockFunction[Future[String]]

      fn expects () returning promise.future once

      singleFlight.execute("key", fn)
      singleFlight.execute("key", fn)
      singleFlight.execute("key", fn)
      promise.success("value")
      Await.result(promise.future, 0 nanos)
    }

    "not limit function invocations when executed against different keys" in {
      val singleFlight = new SingleFlight[String, String]
      val promise = Promise[String]()
      val fn = mockFunction[Future[String]]

      fn expects () returning promise.future twice

      singleFlight.execute("key1", fn)
      singleFlight.execute("key1", fn)
      singleFlight.execute("key2", fn)
      promise.success("value")
      Await.result(promise.future, 0 nanos)
    }
  }
}

