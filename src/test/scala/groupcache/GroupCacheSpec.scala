package groupcache

import org.scalatest._
import matchers.ShouldMatchers

class GroupCacheSpec extends FlatSpec with ShouldMatchers {
  "A group cache" should "get" in {
    val cache = new GroupCache
    cache.get(5) should equal (6)
  }
}

