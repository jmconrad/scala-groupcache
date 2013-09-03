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

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import groupcache.Implicits._
import java.net.URL

class ImplicitsSpec extends FlatSpec with ShouldMatchers {
  "A string" should "be implicitly converted into a URL" in {
    val string = "http://localhost:8080"
    val url: URL = string

    url.getProtocol should equal ("http")
    url.getHost should equal ("localhost")
    url.getPort should equal (8080)
  }

  "A string" should "be implicitly converted into a byte view" in {
    val string = "test"
    val byteView: ByteView = string
    byteView.toString should equal (string)
  }

  "A byte view" should "be implicitly converted into a string" in {
    val byteView = ByteView("test")
    val string: String = byteView
    string should equal (byteView.toString)
  }

  "A byte array" should "be implicitly converted into a byte view" in {
    val byteArray = Array[Byte]('t', 'e', 's', 't')
    val byteView: ByteView = byteArray
    byteView.toString should equal ("test")
  }

  "A byte view" should "be implicitly converted into a byte array" in {
    val byteView = ByteView("test")
    val byteArray: Array[Byte] = byteView
    byteArray should equal ("test".getBytes("UTF-8"))
  }
}

