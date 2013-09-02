package examples.basicusage

import scala.concurrent.{Await, future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import groupcache._
import groupcache.peers.http.{HttpPeer, HttpPeerPicker}
import groupcache.Implicits._
import java.net.URL

object BasicUsage extends App {
  // URL of the peer running in the current process.
  val myUrl = "http://localhost:8080"

  // URLs of all peers that will participate in the group.
  val allUrls = Array[URL]("http://localhost:8080")

  // Determines which peer owns a particular key.  The set of available
  // peers can be updated at any time to handle scenarios where the
  // available peers will change dynamically.
  val peerPicker = new HttpPeerPicker(myUrl, allUrls)

  // Manages groups that are currently participating in a distributed cache.
  // When creating the group cache, you can optionally pass in a callback
  // to be notified when the server has "started" (i.e., when the first
  // group has been added), as well as a callback to be notified each time
  // any group is added.
  implicit val groupCache = GroupCache(peerPicker)

  // Getters fill the cache when a key is requested from the peer
  // that owns that key and that key's value is not already cached
  // in-memory by that peer.  An example of an operation that would
  // be performed in a getter is the retrieval of a value from a
  // database.
  val expensiveGetter = (key: String, context: Option[Any]) => {
    val result = future {
      // Potentially long-running operation that eventually returns
      // a value to be stored in the cache.
      "value"
    }

    // Map the fetched value into a form usable by the cache.
    result.map(value => ByteView(value))
  }

  // Adds the first group that will participate in caching.
  val group = groupCache.addGroup(
    name = "group",
    maxCacheBytes = 64<<20,
    getter = expensiveGetter)

  // An instance of the peer running in the current process.
  val peer = new HttpPeer(myUrl)

  // Start listening for requests for cache values over HTTP.
  peer.serveHttp

  // Ask the group for the cached value associated with the given key.
  // If it is determined that the peer running in the current
  // process is the owner (or the value is in the current process'
  // hot cache), the value will be fetched locally.  Otherwise,
  // the value will be fetched from a (potentially remote) peer
  // over HTTP.
  val futureValue = group.get("key")

  // Register callbacks to determine when the value has been
  // fetched from the cache.
  futureValue onComplete {
    case Success(value) => {
      // Do something useful with the requested value.

      println(s"Received value '$value'")
    }
    case Failure(t) => {
      // Failed to fetch the cached value.  Handle
      // the exception appropriately here.
    }
  }

  // Blocks until the async get has completed so the fetched
  // value can be printed before the program exits.  Don't
  // do this in real code.
  Await.result(futureValue, 500 millis)
}

