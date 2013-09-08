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

package groupcache

import java.net.URL
import scala.language.implicitConversions

object Implicits {
  /**
   * Implicitly converts a string to a URL.  Convenient when
   * constructing peers and groups.
   * @param string
   * @return
   */
  implicit def string2Url(string: String): URL = new URL(string)

  /**
   * Implicitly converts a string to a byte view.
   * @param string
   * @return
   */
  implicit def string2ByteView(string: String): ByteView = ByteView(string)

  /**
   * Implicitly converts a byte view to a string.
   * @param byteView
   * @return
   */
  implicit def byteView2String(byteView: ByteView): String = byteView.toString

  /**
   * Implicitly converts a byte array to a byte view.
   * @param byteArray
   * @return
   */
  implicit def byteArray2ByteView(byteArray: Array[Byte]): ByteView = ByteView(byteArray)

  /**
   * Implicitly converts a byte view to a byte array.
   * @param byteView
   * @return
   */
  implicit def byteView2ByteArray(byteView: ByteView): Array[Byte] = byteView.byteSlice
}

