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

package groupcache.sinks

import com.google.protobuf.MessageLite
import groupcache.util.ByteView

class StringSink(private val dst: StringBuilder) extends Sink {
  private[groupcache] val view = ByteView(dst.mkString)

  def setString(str: String): Unit = {
    dst.clear
    dst.append(str)
    view.value = Right(str)
  }

  def setBytes(bytes: Array[Byte]): Unit = {
    setString(new String(bytes))
  }

  def setProto(msg: MessageLite) {
    val bytes = msg.toByteArray
    dst.clear
    dst.append(new String(bytes))
    view.value = Left(bytes)
  }
}

