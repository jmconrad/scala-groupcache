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

import com.twitter.finagle.{Service, Filter}
import com.twitter.util.Future
import org.jboss.netty.handler.codec.http.{HttpResponse, HttpRequest}
import groupcache.group.GroupRegister
import java.net.{URLDecoder, URI}

/**
 * Validates incoming HTTP requests.
 */
private[groupcache] class RequestValidator(
  private[this] val groupRegister: GroupRegister,
  private[this] val basePath: String) extends Filter[HttpRequest, HttpResponse, GroupHttpRequest, HttpResponse] {

  def apply(request: HttpRequest, continue: Service[GroupHttpRequest, HttpResponse]): Future[HttpResponse] = {
    val uri = new URI(request.getUri)
    val path = uri.getPath

    if (!path.startsWith(basePath)) {
      return Future.exception(new InvalidPathException(s"Unknown path: $path"))
    }

    val parts = path.split("/")
    if (parts.length != 4) {
      return Future.exception(new InvalidPathException(s"Invalid path: $path"))
    }

    val groupName = URLDecoder.decode(parts(2), "UTF-8")
    val key = URLDecoder.decode(parts(3), "UTF-8")

    groupRegister.getGroup(groupName) match {
      case Some(group) => continue(GroupHttpRequest(key, group, request))
      case _ => Future.exception(new GroupNotFoundException(s"Group not found: $groupName"))
    }
  }
}

