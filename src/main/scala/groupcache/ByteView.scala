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

/**
 * A ByteView holds an immutable view of bytes.
 * @param value An array of bytes.
 */
class ByteView private[groupcache](val value: Array[Byte]) extends AnyVal {
  /**
   * Gets the length of this view.
   */
  def length: Int = {
    value.length
  }

  /**
   * Gets a copy of the data as a byte slice.
   */
  def byteSlice: Array[Byte] = {
    value.clone
  }

  /**
   * Gets a string representation of this view.
   */
  override def toString: String = {
    new String(value)
  }

  /**
   * Gets the byte at the given index.
   */
  def at(index: Int): Byte = {
    value(index)
  }

  /**
   * Gets a new ByteView containing the bytes in the given range.
   */
  def slice(from: Int, to: Int): ByteView = {
    new ByteView(value.slice(from, to))
  }

  /**
   * Gets a new ByteView containing the bytes starting from the given index.
   */
  def sliceFrom(from: Int): ByteView = {
    new ByteView(value.drop(from))
  }

  /**
   * Copies the contents of this view into the given byte array.  If the
   * array is smaller than the size of this view, it will copy as much
   * of this view as the array will hold.
   */
  def copy(dest: Array[Byte]): Unit = {
    value.copyToArray(dest)
  }

  /**
   * Determines if the given byte array contains the same
   * byte elements as this view.
   */
  def sameElements(that: ByteView): Boolean = {
    value.sameElements(that.value)
  }
}

/**
 * A ByteView holds an immutable view of bytes.
 */
object ByteView {
  /**
   * Constructs a ByteView out of the given string.
   */
  def apply(str: String) = new ByteView(str.getBytes("UTF-8"))

  /**
   * Constructs a ByteView out of the given byte array.
   */
  def apply(bytes: Array[Byte]) = new ByteView(bytes)
}

