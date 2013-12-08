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

import com.twitter.finagle.Service
import com.twitter.util.{Promise, Future}
import com.google.protobuf.ByteString
import org.jboss.netty.handler.codec.http.{DefaultHttpResponse, HttpResponse, HttpRequest}
import org.jboss.netty.handler.codec.http.HttpVersion._
import org.jboss.netty.handler.codec.http.HttpResponseStatus._
import org.jboss.netty.buffer.ChannelBuffers._
import scala.util._
import scala.concurrent.ExecutionContext.Implicits.global
import groupcachepb.GetResponse

/**
 * Serves cached values over HTTP.
 *
 * @param contextFn Optional callback that allows for a user to specify context for
 *                  this peer (when acting as a server) when a request is received.
 *                  The callback accepts an HttpRequest as a parameter and returns
 *                  any optional value that this peer will treat as opaque.
 */
private[groupcache] class CacheService(
  private[this] val contextFn: Option[(HttpRequest) => Option[Any]] = None) extends Service[GroupHttpRequest, HttpResponse] {

  def apply(groupRequest: GroupHttpRequest): Future[HttpResponse] = {
    val context: Option[Any] = contextFn match {
      case Some(fn) => {
        fn(groupRequest.rawRequest)
      }
      case _ => None
    }

    groupRequest.group.groupStats.serverRequests.incrementAndGet()
    val futureValue = groupRequest.group.get(groupRequest.key, context)
    val promise = new Promise[HttpResponse]()

    futureValue onComplete {
      case Success(byteView) => {
        try {
          val getResponse = new GetResponse(Some(ByteString.copyFrom(byteView.byteSlice)))
          val successResponse = new DefaultHttpResponse(HTTP_1_1, OK)
          successResponse.setHeader("Content-Type", "application/x-protobuf")
          successResponse.setContent(copiedBuffer(getResponse.toByteArray))
          promise.setValue(successResponse)
        }
        catch {
          case t: Throwable => promise.setException(t)
        }
      }
      case Failure(t) => promise.setException(t)
    }

    promise
  }
}

