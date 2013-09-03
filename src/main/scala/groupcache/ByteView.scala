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
 * A ByteView holds an immutable view of bytes
 * @param value Either a byte array or a string that will be exposed as a view of bytes
 */
class ByteView(private val value: Either[Array[Byte], String]) /*extends AnyVal*/ {
  /**
   * Gets the length of this view
   */
  def length: Int = value match {
    case v if v.isLeft => v.left.get.length
    case v => v.right.get.getBytes("UTF-8").length
  }

  /**
   * Gets a copy of the data as a byte slice
   */
  def byteSlice: Array[Byte] = value match {
    case v if v.isLeft => v.left.get.clone()
    case v => v.right.get.getBytes("UTF-8")
  }

  /**
   * Gets a string representation of this view
   */
  override def toString: String = value match {
    case v if v.isLeft => new String(v.left.get)
    case v => v.right.get
  }

  /**
   * Gets the byte at the given index
   */
  def at(index: Int): Byte = value match {
    case v if v.isLeft => v.left.get(index)
    case v => v.right.get.charAt(index).toByte
  }

  /**
   * Gets a new ByteView containing the bytes in the given range
   */
  def slice(from: Int, to: Int): ByteView = value match {
    case v if v.isLeft => new ByteView(Left(v.left.get.slice(from, to)))
    case v => new ByteView(Right(v.right.get.substring(from, to)))
  }

  /**
   * Gets a new ByteView containing the bytes starting from the given index
   */
  def sliceFrom(from: Int): ByteView = value match {
    case v if v.isLeft => new ByteView(Left(v.left.get.drop(from)))
    case v => new ByteView(Right(v.right.get.drop(from)))
  }

  /**
   * Copies the contents of this view into the given byte array.  If the
   * array is smaller than the size of this view, it will copy as much
   * of this view as the array will hold
   */
  def copy(dest: Array[Byte]): Unit = value match {
    case v if v.isLeft => v.left.get.copyToArray(dest)
    case v => v.right.get.getBytes("UTF-8").copyToArray(dest)
  }

  /**
   * Gets the hash code of this view
   */
  override def hashCode: Int = value match {
    case v if v.isLeft => v.left.get.hashCode
    case v => v.right.get.hashCode
  }

  /**
   * Determine if the given object is equal to this view
   */
  override def equals(other: Any): Boolean = value match {
    case v if v.isLeft && other.isInstanceOf[ByteView] => other.asInstanceOf[ByteView].equalsBytes(v.left.get)
    case v if other.isInstanceOf[ByteView] => other.asInstanceOf[ByteView].equalsString(v.right.get)
    case _ => false
  }

  /**
   * Determines if the given string is equal to this view
   */
  private def equalsString(str: String): Boolean = {
    if (value.isRight) {
      return value.right.get.equals(str)
    }

    val bytes = value.left.get
    if (str.length != bytes.length) {
      return false
    }

    val mismatch = bytes.view.zipWithIndex.exists(elem => elem._1 != str.charAt(elem._2))
    if (mismatch) {
      return false
    }

    true
  }

  /**
   * Determines if the given byte array is equal to this view
   */
  private def equalsBytes(bytes: Array[Byte]): Boolean = {
    if (value.isLeft) {
      return bytes.deep == value.left.get.deep
    }

    val str = value.right.get
    if (str.length != bytes.length) {
      return false
    }

    val mismatch = bytes.view.zipWithIndex.exists(elem => elem._1 != str.charAt(elem._2))
    if (mismatch) {
      return false
    }

    true
  }
}

/**
 * Companion object containing convenience methods
 */
object ByteView {
  /**
   * Constructs a ByteView out of the given string
   */
  def apply(str: String) = new ByteView(Right(str))

  /**
   * Constructs a ByteView out of the given byte array
   */
  def apply(bytes: Array[Byte]) = new ByteView(Left(bytes))
}

