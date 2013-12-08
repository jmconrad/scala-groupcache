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

package groupcache.hash

import java.util.zip.CRC32
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Map

/**
 * Implementation of a ring hash that allows peers to be chosen in a
 * consistent manner.  Instances of this class are not safe for
 * concurrent access.
 *
 * @param replicas The replication count applied to each key in the ring.
 *                 This is used to distribute keys among peers more evenly.
 *
 * @param hashFn An optional hash function to use.  If the hash function
 *               is not provided the IEEE CRC32 checksum implementation
 *               is used.
 */
class RingHash(private val replicas: Int, hashFn: Option[Array[Byte] => Long] = None) {

  if (replicas <= 0) {
    throw new IllegalArgumentException("replicas must be at least 1")
  }

  private val hashMap = Map[Int, String]()
  private var sortedKeys = ListBuffer[Int]()

  // Helps determine which peer will own a key.
  private val fn: Array[Byte] => Long = hashFn match {
    case Some(someFn) => someFn
    case None => {
      // Default to the IEEE CRC32 checksum implementation.
      def checksum(bytes: Array[Byte]): Long = {
        val crc = new CRC32
        crc.update(bytes, 0, bytes.length)
        crc.getValue
      }

      checksum
    }
  }

  /**
   * Determines if there are no items available in the ring hash.
   */
  def isEmpty: Boolean = hashMap.isEmpty

  /**
   * Adds the given keys to the hash.
   */
  def add(keys: String*): Unit = {
    keys.foreach(key => {
      for (i <- 0 until this.replicas) {
        val hash = computeHash(i.toString + key)
        this.hashMap(hash) = key
        this.sortedKeys.append(hash)
      }

      this.sortedKeys = this.sortedKeys.sorted
    })
  }

  /**
   * Gets the "closest" entry in the hash using the given key.
   */
  def get(key: String): String = {
    if (this.isEmpty) {
      ""
    }
    else {
      val computedHash = computeHash(key)

      this.sortedKeys.find(elem => elem >= computedHash) match {
        case Some(hash) => this.hashMap(hash)
        case None => {
          // Cycled back to the first replica.
          this.hashMap(this.sortedKeys.head)
        }
      }
    }
  }

  /**
   * Computes the hash value of the given key using the underlying
   * hash function.
   */
  private def computeHash(key: String): Int = {
    val bytes = key.getBytes("UTF-8")
    this.fn(bytes).toInt
  }
}

