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
import com.twitter.finagle.Service
import org.jboss.netty.handler.codec.http.{DefaultHttpResponse, HttpResponse, HttpRequest}
import com.twitter.util.{Await, Future}
import org.jboss.netty.handler.codec.http.HttpVersion._
import org.jboss.netty.handler.codec.http.HttpResponseStatus._
import groupcache.group.GroupRegister
import groupcache.group.Group

class RequestValidatorSpec extends WordSpec with ShouldMatchers with MockFactory {
  val basePath = "/_groupcache"

  // Can't use scalamock to mock out the service since it has final members.
  val mockService = new Service[GroupHttpRequest, HttpResponse] {
    def apply(request: GroupHttpRequest): Future[HttpResponse] = {
      Future.value(new DefaultHttpResponse(HTTP_1_1, OK))
    }
  }

  def runInvalidPathTest(path: String) = {
    val mockRequest = mock[HttpRequest]
    (mockRequest.getUri _).expects().returning(path)

    val mockRegister = mock[GroupRegister]
    val validator = new RequestValidator(mockRegister, basePath)
    val futureResponse = validator(mockRequest, mockService)

    intercept[InvalidPathException] {
      Await.result(futureResponse)
    }
  }

  "A request validator" should {
    "throw an invalid path exception when the base URI path does not start with the known base path" in {
      runInvalidPathTest("/invalid/path")
    }

    "throw an invalid path exception when the URI path is not of the form {base}/{groupname}/{key}" in {
      runInvalidPathTest(s"$basePath/groupname")
    }

    "throw a group not found exception when the URI path references a non-existent group name" in {
      val mockRequest = mock[HttpRequest]
      (mockRequest.getUri _).expects().returning(s"$basePath/groupname/key")

      val mockRegister = mock[GroupRegister]
      (mockRegister.getGroup _).expects("groupname").returning(None)

      val validator = new RequestValidator(mockRegister, basePath)
      val futureResponse = validator(mockRequest, mockService)

      intercept[GroupNotFoundException] {
        Await.result(futureResponse)
      }
    }

    "let the request pass through when a valid path referencing a valid group is provided" in {
      val mockRequest = mock[HttpRequest]
      (mockRequest.getUri _).expects().returning(s"$basePath/groupname/key")

      class MockGroup extends Group("groupname", null, null, 0, 0)
      val mockGroup = mock[MockGroup]
      val mockRegister = mock[GroupRegister]

      (mockRegister.getGroup _).expects("groupname").returning(Some(mockGroup))

      val validator = new RequestValidator(mockRegister, basePath)
      val futureResponse = validator(mockRequest, mockService)

      Await.result(futureResponse)
    }
  }
}

