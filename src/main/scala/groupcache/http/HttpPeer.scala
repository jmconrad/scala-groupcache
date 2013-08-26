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

import groupcachepb.{GetResponse, GetRequest}
import java.net.{URL, URLEncoder}
import com.twitter.finagle.builder.ClientBuilder
import com.twitter.finagle.http.Http
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.codec.http.HttpResponseStatus.OK
import concurrent.{Promise, Future}
import groupcache.peers.{PeerPicker, Peer}
import java.util.zip.CRC32
import java.util.concurrent.locks.ReentrantLock
import groupcache.group.GroupRegister

class HttpPeerException(msg: String) extends Exception(msg)

class HttpPeer(private val baseUrl: URL,
               private val basePath: String = "/_groupcache",
               private var peerUrls: Array[URL],
               private val groupRegister: GroupRegister) extends Peer with PeerPicker {

  private val lock = new ReentrantLock()
  private val host = baseUrl.getHost()

  private val port = baseUrl.getPort() match {
    case p if p < 0 => 80
    case p => p
  }

  override def get(context: Option[Any] = None, request: GetRequest): Future[GetResponse] = {
    val group = URLEncoder.encode(request.`group`, "UTF-8")
    val key = URLEncoder.encode(request.`key`, "UTF-8")
    val path = s"$basePath/$group/$key"
    val hostAndPort = s"$host:$port"
    val httpClient = ClientBuilder().codec(Http()).hosts(hostAndPort).hostConnectionLimit(1).build()
    val httpRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, path)
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

  override def pickPeer(key: String): Option[Peer] = {
    val sum = checksum(key)
    var pickedPeer: Option[Peer] = None

    lock.lock()
    try {
      if (this.peerUrls.length > 0) {
        this.peerUrls(sum % this.peerUrls.length) match {
          case peerBaseUrl if peerBaseUrl != baseUrl => {
            pickedPeer = Some(new HttpPeer(peerBaseUrl, this.basePath, this.peerUrls, this.groupRegister))
          }
        }
      }
    }
    finally {
      lock.unlock()
    }

    pickedPeer
  }

  def setPeerUrls(peerUrls: Array[URL]): Unit = {
    lock.lock()
    try {
      this.peerUrls = peerUrls
    }
    finally {
      lock.unlock()
    }
  }

  private def checksum(key: String): Int = {
    val crc = new CRC32
    val bytes = key.getBytes
    crc.update(bytes, 0, bytes.length)
    crc.getValue.toInt
  }
}

