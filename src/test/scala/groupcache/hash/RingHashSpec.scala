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

import org.scalatest._
import matchers.ShouldMatchers
import scala.collection.mutable.Map

class RingHashSpec extends WordSpec with ShouldMatchers {
  val replicas = 3
  def mockHashFn(bytes: Array[Byte]): Long = new String(bytes).toLong
  val ringHash = new RingHash(replicas, Some(mockHashFn))

  ringHash.add("6", "4", "2")
  val baseTestCases = Map("2" -> "2", "11" -> "2", "23" -> "4", "27" -> "2")

  "A ring hash" should {
    "get the correct hash values for the base test cases" in {
      baseTestCases.foreach {
        case (key, value) => {
          ringHash.get(key) should equal (value)
        }
      }
    }

    "allow a key to be added" in {
      val testCases = Map[String, String]() ++= baseTestCases

      ringHash.add("8")
      testCases("27") = "8"

      testCases.foreach {
        case (key, value) => {
          ringHash.get(key) should equal (value)
        }
      }
    }

    "ensure keys are hashed consistently" in {
      val ringHash1 = new RingHash(1)
      val ringHash2 = new RingHash(1)
      ringHash1.add("Bill", "Bob", "Bonny")
      ringHash2.add("Bob", "Bonny", "Bill")

      ringHash1.get("Ben") should equal (ringHash2.get("Ben"))

      ringHash2.add("Becky", "Ben", "Bobby")

      ringHash1.get("Ben") should equal (ringHash2.get("Ben"))
      ringHash1.get("Bob") should equal (ringHash2.get("Bob"))
      ringHash1.get("Bonny") should equal (ringHash2.get("Bonny"))
    }
  }
}

