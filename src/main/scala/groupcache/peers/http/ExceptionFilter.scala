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

import com.twitter.finagle.{Service, SimpleFilter}
import org.jboss.netty.handler.codec.http.{DefaultHttpResponse, HttpResponse, HttpRequest}
import org.jboss.netty.handler.codec.http.HttpResponseStatus.{BAD_REQUEST, NOT_FOUND, INTERNAL_SERVER_ERROR}
import org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1
import org.jboss.netty.util.CharsetUtil.UTF_8
import org.jboss.netty.buffer.ChannelBuffers.copiedBuffer

/**
 * Handles construction of error responses when an exception occurs
 * attempting to service a cached value over HTTP.
 */
private[groupcache] class ExceptionFilter extends SimpleFilter[HttpRequest, HttpResponse] {
  override def apply(request: HttpRequest, service: Service[HttpRequest, HttpResponse]) = {
    service(request) handle { case error =>
      val statusCode = error match {
        case _: InvalidPathException =>
          BAD_REQUEST
        case _: GroupNotFoundException =>
          NOT_FOUND
        case _ =>
          INTERNAL_SERVER_ERROR
      }

      val errorResponse = new DefaultHttpResponse(HTTP_1_1, statusCode)
      errorResponse.setContent(copiedBuffer(error.getStackTraceString, UTF_8))

      errorResponse
    }
  }
}

