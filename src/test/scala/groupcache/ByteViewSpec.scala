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

import org.scalatest._
import matchers.ShouldMatchers

class ByteViewSpec extends FlatSpec with ShouldMatchers {
  "A byte view" should "have a length of 5 when constructed from a 5 character string" in {
    val view = ByteView("12345")
    view.length should equal (5)
  }

  "A byte view" should "have a length of 5 when constructed from an array of 5 bytes" in {
    val bytes = Array[Byte](1, 2, 3, 4, 5)
    val view = ByteView(bytes)
    view.length should equal (5)
  }

  "A byte view containing a string" should "return a copy of the data as a byte slice" in {
    val str = "12345"
    val byteArray = str.map(elem => elem.toByte).toArray
    val view = ByteView(str)
    view.byteSlice.deep should equal (byteArray.deep)
  }

  "A byte view containing a byte array" should "return a copy of the data as a byte slice" in {
    val bytes = Array[Byte](1, 2, 3, 4, 5)
    val view = ByteView(bytes)
    view.byteSlice should not equal (bytes)
    view.byteSlice.deep should equal (bytes.deep)
  }

  "A byte view containing a string" should "have a string representation equal to that string" in {
    val str = "some string"
    val view = ByteView(str)
    view.toString should equal (str)
  }

  "A byte view containing a byte array" should "have a string representation of that byte array" in {
    val bytes = Array[Byte]('t', 'e', 's', 't')
    val view = ByteView(bytes)
    view.toString should equal (new String(bytes))
  }

  "A byte view containing a string" should "return its second character when at(1) is called" in {
    val view = ByteView("test")
    view.at(1) should equal ('e'.toByte)
  }

  "A byte view containing a byte array" should "return its second element when at(1) is called" in {
    val bytes = Array[Byte](1, 2, 3, 4, 5)
    val view = ByteView(bytes)
    view.at(1) should equal (2.toByte)
  }

  "A byte view containing a string" should "return its 2-3 characters when slice(1, 3) is called" in {
    val view = ByteView("test_string")
    view.slice(1, 3).toString should equal ("es")
  }

  "A byte view containing a byte array" should "return its 2-3 characters when slice(1, 3) is called" in {
    val bytes = Array[Byte](1, 2, 3, 4, 5)
    val slice = Array[Byte](2, 3)
    val view = ByteView(bytes)
    assert(view.slice(1, 3) === ByteView(slice))
  }

  "A byte view containing a string" should "return all but its first 2 characters when sliceFrom(2) is called" in {
    val view = ByteView("test_string")
    view.sliceFrom(2).toString should equal("st_string")
  }

  "A byte view containing a byte array" should "return all but its first 2 elements when sliceFrom(2) is called" in {
    val bytes = Array[Byte](1, 2, 3, 4, 5)
    val slice = Array[Byte](3, 4, 5)
    val view = ByteView(bytes)
    assert(view.sliceFrom(2) === ByteView(slice))
  }

  "A byte view containing a string" should "copy the string to a byte array" in {
    val str = "test_string"
    val view = ByteView(str)
    val dest = new Array[Byte](view.length)
    view.copy(dest)
    dest.deep should equal (str.map(elem => elem.toByte).toArray.deep)
  }

  "A byte view containing a string" should "copy as much of the string as it can to a byte array with a short length" in {
    val str = "test_string"
    val view = ByteView(str)
    val dest = new Array[Byte](view.length - 2)
    view.copy(dest)
    dest.deep should equal ("test_stri".map(elem => elem.toByte).toArray.deep)
  }

  "A byte view containing a string" should "copy all of the string into a byte array larger than the string" in {
    val str = "test"
    val view1 = ByteView(str)
    val dest = new Array[Byte](view1.length + 10)
    view1.copy(dest)

    dest.take(str.length).deep should equal(str.map(elem => elem.toByte).toArray.deep)
  }

  "A byte view containing a byte array" should "copy its contents to a byte array" in {
    val bytes = Array[Byte]('t', 'e', 's', 't')
    val view = ByteView(bytes)
    val dest = new Array[Byte](view.length)
    view.copy(dest)
    dest.deep should equal (bytes.deep)
  }

  "A byte view containing a string" should "use the string's hash code" in {
    val str = "test_string"
    val view = ByteView(str)
    view.hashCode should equal (str.hashCode)
  }

  "A byte view containing a byte array" should "use the array's hash code" in {
    val bytes = Array[Byte]('t', 'e', 's', 't')
    val view = ByteView(bytes)
    view.hashCode should equal (bytes.hashCode)
  }

  "A byte view containing a string" should "equal another byte view containing the same string" in {
    val view1 = ByteView("test")
    val view2 = ByteView("test")
    assert(view1 === view2)
  }

  "A byte view containing a string" should "equal another byte view containing a byte array with the same content" in {
    val strContent = "test_string"
    val bytesContent = strContent.map(elem => elem.toByte).toArray
    val view1 = ByteView(strContent)
    val view2 = ByteView(bytesContent)
    assert(view1 === view2)
  }

  "A byte view containing a string" should "not equal another byte view containing a byte array with different content" in {
    val view1 = ByteView("test_string")
    val view2 = ByteView(Array[Byte](1, 2, 3, 4, 5))
    val test = view1.equals(view2)
    test should equal (false)
  }

  "A byte view containing a byte array" should "equal another byte view containing the same array content" in {
    val view1 = ByteView(Array[Byte]('t', 'e', 's', 't'))
    val view2 = ByteView(Array[Byte]('t', 'e', 's', 't'))
    assert(view1 === view2)
  }

  "A byte view" should "not equal an object that is not of type byte view" in {
    val test = ByteView("test").equals(2)
    test should equal (false)
  }
}

