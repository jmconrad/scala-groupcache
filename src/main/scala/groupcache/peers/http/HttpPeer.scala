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

import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.codec.http.HttpResponseStatus.{OK}
import groupcachepb.{GetResponse, GetRequest}
import groupcache.group.Group
import groupcache.peers.Peer
import groupcache.group.GroupRegister
import java.net._
import com.twitter.conversions.time._
import com.twitter.finagle.builder.{ServerBuilder, ClientBuilder}
import com.twitter.finagle.http.Http
import HttpMethod.GET
import HttpVersion.HTTP_1_1
import scala.concurrent._

private[groupcache] case class GroupHttpRequest(val key: String, val group: Group, val rawRequest: HttpRequest)
class HttpPeerException(msg: String, cause: Throwable = null) extends Exception(msg, cause)
class InvalidPathException(msg: String, cause: Throwable = null) extends HttpPeerException(msg, cause)
class GroupNotFoundException(msg: String, cause: Throwable = null) extends HttpPeerException(msg, cause)

/**
 * Peer that acts as a client and optionally a server for fetching and providing cached values over HTTP.
 *
 * @param baseUrl Base URL of this peer.
 * @param contextFn Optional callback that allows for a user to specify context for
 *                  this peer (when acting as a server) when a request is received.
 *                  The callback accepts an HttpRequest as a parameter and returns
 *                  any optional value that this peer will treat as opaque.
 */
class HttpPeer(private[this] val baseUrl: URL,
               private[this] val contextFn: Option[(HttpRequest) => Option[Any]] = None) extends Peer {

  private[this] val basePath: String = "/_groupcache"
  private[this] val host = baseUrl.getHost()

  private[this] val port = baseUrl.getPort() match {
    case p if p < 0 => 80
    case p => p
  }

  /**
   * Constructs an HTTP peer using the given port.  This constructor should only
   * be used when this peer corresponds to localhost.
   * @param localPort
   * @param localContextFn
   */
  def this(localPort: Int, localContextFn: Option[(HttpRequest) => Option[Any]]) {
    this(new URL(s"http://localhost:$localPort"), localContextFn)
  }

  /**
   * Asynchronously gets a cached value from a peer over HTTP.
   * @param request Protobuf-encoded request containing group name and key.
   * @param context Optional, opaque context data
   * @return A future protobuf-encoded response served over HTTP.
   */
  override def get(request: GetRequest, context: Option[Any] = None): Future[GetResponse] = {
    val group = URLEncoder.encode(request.`group`, "UTF-8")
    val key = URLEncoder.encode(request.`key`, "UTF-8")
    val path = s"$basePath/$group/$key"
    val hostAndPort = s"$host:$port"
    val httpClient = ClientBuilder()
      .codec(Http())
      .hosts(hostAndPort)
      .hostConnectionLimit(1)
      .tcpConnectTimeout(1.second)
      .retries(0)
      .build()

    val httpRequest = new DefaultHttpRequest(HTTP_1_1, GET, path)
    val responseFuture = httpClient(httpRequest)
    val promise = Promise[GetResponse]()

    responseFuture onSuccess {
      httpResponse => {
        val status = httpResponse.getStatus

        if (status != OK) {
          val code = status.getCode
          val msg = s"Error getting response from HTTP peer.  Received HTTP code: $code"
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
      case t: Throwable => {
        val msg = t.getMessage
        val exception = new HttpPeerException(s"Error communicating with HTTP peer.  Received error: $msg", t)
        promise.failure(exception)
      }
    }

    promise.future
  }

  /**
   * Serves protobuf-encoded cache values over HTTP.  Valid requests contain a URL
   * path of the form /_groupcache/{groupName}/{key}
   *
   * @param groupRegister Allows a group to be located by name.
   */
  def serveHttp(implicit groupRegister: GroupRegister): Unit = {
    val exceptionFilter = new ExceptionFilter
    val validationFilter = new RequestValidator(groupRegister, basePath)
    val responder = new CacheService(contextFn)

    val service = exceptionFilter andThen validationFilter andThen responder

    try {
      ServerBuilder()
        .codec(Http())
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
}

