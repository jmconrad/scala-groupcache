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

import collection.mutable.ArrayBuffer
import com.google.protobuf.MessageLite
import groupcache.ByteView

class TruncatingByteSliceSink(private val dst: ArrayBuffer[Byte], private val size: Int) extends Sink {
  private val view = ByteView(dst.toArray)

  def setString(str: String): Unit = {
    val truncatedBytes = str.getBytes.take(size)
    dst.clear
    dst.prependAll(truncatedBytes)
    view.value = Right(str)
  }

  def setBytes(bytes: Array[Byte]): Unit = {
    val truncatedBytes = bytes.take(size)
    dst.clear
    dst.prependAll(truncatedBytes.clone)
    view.value = Left(truncatedBytes)
  }

  def setProto(msg: MessageLite): Unit = {
    val truncatedBytes = msg.toByteArray.take(size)
    dst.clear
    dst.prependAll(truncatedBytes.clone)
    view.value = Left(truncatedBytes)
  }
}

