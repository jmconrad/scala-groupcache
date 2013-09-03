package examples.basic

import scala.concurrent.{Await, future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import groupcache._
import groupcache.peers.http.{HttpPeer, HttpPeerPicker}
import java.net.URL
import java.util.Date
import org.jboss.netty.handler.codec.http.HttpRequest

/**
 * Run this example from sbt using the 'run' task.  Pass as parameters the index
 * of the current peer's URL, as well as all of the URL's that will participate.
 *
 * For example, from sbt on the console:
 * run 0 http://localhost:9001 http://localhost:9002 http://localhost:9003 http://localhost:9004
 *
 * This will expect 4 peers to participate, with index 0 (http://localhost:9001) corresponding
 * to the current peer.  You can of course run multiple peers by running multiple instances
 * of sbt on the terminal and specifying a different peer index for each.
 */
object BasicUsage extends App {
  if (args.length < 2) {
    println("Please provide the index of this peer and at least one URL.")
    System.exit(1)
  }

  val myPeerIndex = args(0).toInt
  if (myPeerIndex > args.length - 2) {
    println("Peer index is out of range.")
    System.exit(1)
  }

  val allUrls: Array[URL] = args.takeRight(args.length - 1).map(new URL(_))
  val myUrl = allUrls(myPeerIndex)

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
      println(s"Getter called to retrieve key '$key'")

      val dt = new Date
      s"Value for key '$key' loaded at $dt on the peer with URL $myUrl"
    }

    // Map the fetched value into a form usable by the cache.
    result.map(value => ByteView(value))
  }

  // Adds the first group that will participate in caching.
  val group = groupCache.addGroup(
    name = "group",
    maxCacheBytes = 64<<20,
    getter = expensiveGetter)

  // The context function generates contextual data for a peer
  // when serving a cached value over HTTP.  The contextual data
  // is passed through to that peer's getter.
  val contextFn = (request: HttpRequest) => {
    println(s"Request was made for a cache value from $myUrl over HTTP")
    None
  }

  // An instance of the peer running in the current process.
  val peer = new HttpPeer(myUrl, Some(contextFn))

  // Start listening for requests for cache values over HTTP.
  peer.serveHttp

  def fetchValue(keyToFetch: String): Unit = {
    // Ask the group for the cached value associated with the given key.
    // If it is determined that the peer running in the current
    // process is the owner (or the value is in the current process'
    // hot cache), the value will be fetched locally.  Otherwise,
    // the value will be fetched from a (potentially remote) peer
    // over HTTP.
    val futureValue = group.get(keyToFetch)

    // Register callbacks to determine when the value has been
    // fetched from the cache.
    futureValue onComplete {
      case Success(value) => {
        // Do something useful with the requested value.
        println(s"Succesfully received value '$value'")
      }
      case Failure(t) => {
        // Failed to fetch the cached value.  Handle
        // the exception appropriately here.
        val msg = t.getMessage
        println(s"Failed to fetch value: $msg")
      }
    }

    Await.ready(futureValue, 2 seconds)
  }

  while (true) {
    println()
    val keyToFetch = Console.readLine("Enter the name of a key to get from the cache: ")
    fetchValue(keyToFetch)
  }
}

