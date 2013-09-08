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
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.codec.http.HttpResponseStatus.{OK, BAD_REQUEST, NOT_FOUND, INTERNAL_SERVER_ERROR}
import HttpVersion.HTTP_1_1
import com.twitter.finagle.Service
import com.twitter.util.{Await, Future}

class ExceptionFilterSpec extends WordSpec with ShouldMatchers with MockFactory {
  // Can't use scalamock to mock out the service since it has final members.
  def mockServiceFail(throwable: Throwable) = new Service[HttpRequest, HttpResponse] {
    def apply(request: HttpRequest): Future[HttpResponse] = {
      Future.exception(throwable)
    }
  }

  def expectHttpStatus(status: HttpResponseStatus, t: Throwable) = {
    val filter = new ExceptionFilter
    val mockRequest = mock[HttpRequest]
    val filteredFuture = filter(mockRequest, mockServiceFail(t))
    val filtered = Await.result(filteredFuture)

    filtered.getStatus should equal (status)
  }

  "An exception filter" should {
    "respond with an HTTP 400 when an invalid path is encountered" in {
      val invalidPathException = new InvalidPathException("Invalid path");
      expectHttpStatus(BAD_REQUEST, invalidPathException)
    }

    "respond with an HTTP 404 when a non-existent group name is encountered" in {
      val notFoundException = new GroupNotFoundException("Group not found")
      expectHttpStatus(NOT_FOUND, notFoundException)
    }

    "resond with an HTTP 500 when an unknown exception is encountered" in {
      val someException = new Exception
      expectHttpStatus(INTERNAL_SERVER_ERROR, someException)
    }

    "let the response pass through when no exception occurs" in {
      val mockService = new Service[HttpRequest, HttpResponse] {
        def apply(request: HttpRequest): Future[HttpResponse] = {
          Future.value(new DefaultHttpResponse(HTTP_1_1, OK))
        }
      }

      val filter = new ExceptionFilter
      val mockRequest = mock[HttpRequest]
      val filteredFuture = filter(mockRequest, mockService)
      val filtered = Await.result(filteredFuture)

      filtered.getStatus should equal (OK)
    }
  }
}

