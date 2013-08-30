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

package groupcache.http

import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.codec.http.HttpResponseStatus.{OK, BAD_REQUEST, NOT_FOUND}
import org.jboss.netty.util.CharsetUtil.UTF_8
import org.jboss.netty.buffer.ChannelBuffers.copiedBuffer
import groupcachepb.{GetResponse, GetRequest}
import groupcache.peers.Peer
import groupcache.group.GroupRegister
import groupcache.sinks.AllocatingByteSliceSink
import java.net._
import com.twitter.finagle.builder.{ServerBuilder, ClientBuilder}
import com.twitter.finagle.http.Http
import com.twitter.finagle.Service
import com.twitter.util.{Future => FinagleFuture, Promise => FinaglePromise}
import HttpMethod.GET
import HttpVersion.HTTP_1_1
import scala.Some
import collection.mutable.ArrayBuffer
import util.{Failure, Success}
import com.google.protobuf.ByteString
import scala.concurrent._
import ExecutionContext.Implicits.global

class HttpPeerException(msg: String, cause: Throwable = null) extends Exception(msg, cause)

/**
 * Peer that acts as a client and optionally a server for fetching and providing cached values over HTTP
 * @param baseUrl Base URL of this peer
 */
class HttpPeer(private val baseUrl: URL) extends Peer {

  private val host = baseUrl.getHost()
  private val basePath: String = "/_groupcache"

  private val port = baseUrl.getPort() match {
    case p if p < 0 => 80
    case p => p
  }

  /**
   * Asynchronously gets a cached value from this peer over HTTP
   * @param request protobuf-encoded request containing group name and key
   * @param context optional context data
   * @return a future protobuf-encoded response served over HTTP
   */
  override def get(request: GetRequest, context: Option[Any] = None): Future[GetResponse] = {
    val group = URLEncoder.encode(request.`group`, "UTF-8")
    val key = URLEncoder.encode(request.`key`, "UTF-8")
    val path = s"$basePath/$group/$key"
    val hostAndPort = s"$host:$port"
    val httpClient = ClientBuilder().codec(Http()).hosts(hostAndPort).hostConnectionLimit(1).build()
    val httpRequest = new DefaultHttpRequest(HTTP_1_1, GET, path)
    val responseFuture = httpClient(httpRequest)
    val promise = Promise[GetResponse]()

    responseFuture onSuccess {
      httpResponse => {
        val status = httpResponse.getStatus

        if (status != OK) {
          val code = status.getCode
          val msg = s"Error getting response from HTTP peer.  Received HTTP code $code"
          promise.failure(new HttpPeerException(msg))
        }
        else {
          try {
            val content = httpResponse.getContent
            val bytes = new Array[Byte](content.readableBytes)
            content.readBytes(bytes)
            val response = GetResponse.defaultInstance.mergeFrom(bytes)
            promise.success(response)
          }
          catch {
            case t: Throwable => promise.failure(t)
          }
        }
      }
    }

    responseFuture onFailure {
      case t: Throwable => promise.failure(t)
    }

    promise.future
  }

  /**
   * Serves protobuf-encoded cache values over HTTP.  Valid requests contain a URL
   * path of the form /_groupcache/{groupName}/{key}
   *
   *
   * @param groupRegister Allows a group to be located by name
   */
  def serveHttp(implicit groupRegister: GroupRegister): Unit = {
    val service = getHttpService(groupRegister)

    try {
      ServerBuilder().codec(Http())
        .bindTo(new InetSocketAddress(this.port))
        .name("GroupCacheHttpServer")
        .build(service)
    }
    catch {
      case t: Throwable => {
        val msg = t.getMessage()
        throw new HttpPeerException(s"Error starting peer HTTP server: $msg", t)
      }
    }
  }

  /**
   * Constructs a basic Finagle service for serving cache entries over HTTP
   */
  private def getHttpService(groupRegister: GroupRegister): Service[HttpRequest, HttpResponse]  = {
    new Service[HttpRequest, HttpResponse] {
      def apply(request: HttpRequest): FinagleFuture[HttpResponse] = {
        val uri = new URI(request.getUri)
        val path = uri.getPath

        if (!path.startsWith(basePath)) {
          val unknownPathResponse = new DefaultHttpResponse(HTTP_1_1, BAD_REQUEST)
          unknownPathResponse.setContent(copiedBuffer(s"Unknown path: $path", UTF_8))
          return FinagleFuture(unknownPathResponse)
        }

        val parts = path.split("/")
        if (parts.length != 4) {
          val invalidPathResponse = new DefaultHttpResponse(HTTP_1_1, BAD_REQUEST)
          invalidPathResponse.setContent(copiedBuffer(s"Invalid path: $path", UTF_8))
          return FinagleFuture(invalidPathResponse)
        }

        val groupName = URLDecoder.decode(parts(2), "UTF-8")
        val key = URLDecoder.decode(parts(3), "UTF-8")
        val groupOption = groupRegister.getGroup(groupName)

        if (!groupOption.isDefined) {
          val notFoundResponse = new DefaultHttpResponse(HTTP_1_1, NOT_FOUND)
          notFoundResponse.setContent(copiedBuffer(s"Group not found: $groupName", UTF_8))
          return FinagleFuture(notFoundResponse)
        }

        val group = groupOption.get
        val buffer = new ArrayBuffer[Byte]()
        val futureResponse = new FinaglePromise[HttpResponse]()
        val futureValue = group.get(key, new AllocatingByteSliceSink(buffer))

        futureValue onComplete {
          case Success(byteView) => {
            try {
              val getResponse = new GetResponse(Some(ByteString.copyFrom(buffer.toArray)))
              val successResponse = new DefaultHttpResponse(HTTP_1_1, OK)
              successResponse.setHeader("Content-Type", "application/x-protobuf")
              successResponse.setContent(copiedBuffer(getResponse.toByteArray))
              futureResponse.setValue(successResponse)
            }
            catch {
              case t: Throwable => futureResponse.setException(t)
            }
          }
          case Failure(t) => futureResponse.setException(t)
        }

        futureResponse
      }
    }
  }
}

