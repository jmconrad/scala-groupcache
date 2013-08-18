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
import groupcache.ByteView

class ByteViewSink(private val dst: ByteView) extends Sink {
  def setString(str: String): Unit = {
    dst.value = Right(str)
  }

  def setBytes(bytes: Array[Byte]): Unit = {
    dst.value = Left(bytes.clone())
  }

  def setProto(msg: MessageLite): Unit = {
    dst.value = Left(msg.toByteArray)
  }
}

