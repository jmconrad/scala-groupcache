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

import org.scalatest._
import matchers.ShouldMatchers
import org.scalamock.scalatest.MockFactory
import org.jboss.netty.handler.codec.http.HttpRequest
import org.jboss.netty.handler.codec.http.HttpResponseStatus.OK
import groupcache.group.Group
import scala.concurrent.Promise
import groupcache.ByteView
import com.twitter.util.Await

class CacheServiceSpec extends WordSpec with ShouldMatchers with MockFactory {
  "An HTTP cache service" should {
    "invoke a 'context function' if provided" in {
      val mockFn = mockFunction[HttpRequest, Option[Any]]
      val mockRequest = mock[HttpRequest]

      mockFn expects (mockRequest) once

      val service = new CacheService(Some(mockFn))

      // Ignore subsequent exceptions that should occur.
      intercept[Throwable] {
        service(GroupHttpRequest("key", null, mockRequest))
      }
    }

    "return a failed future when a cached value cannot be fetched" in {
      class MockException extends Exception

      val promise = Promise[ByteView]()
      promise.failure(new MockException)

      class MockGroup extends Group("groupname", null, null, 0, 0)
      val mockGroup = mock[MockGroup]
      (mockGroup.get _).expects("key", None).returning(promise.future)

      val mockRequest = mock[HttpRequest]

      val service = new CacheService()
      val futureResponse = service(GroupHttpRequest("key", mockGroup, mockRequest))

      intercept[MockException] {
        Await.result(futureResponse)
      }
    }

    "return a successful future when a cached value is fetched" in {
      val promise = Promise[ByteView]()
      promise.success(ByteView("test"))

      class MockGroup extends Group("groupname", null, null, 0, 0)
      val mockGroup = mock[MockGroup]
      (mockGroup.get _).expects("key", None).returning(promise.future)

      val mockRequest = mock[HttpRequest]
      val service = new CacheService()
      val futureResponse = service(GroupHttpRequest("key", mockGroup, mockRequest))

      val response = Await.result(futureResponse)

      response.getStatus should equal (OK)
    }
  }
}

