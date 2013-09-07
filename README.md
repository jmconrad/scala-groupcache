# scala-groupcache

An implementation of [groupcache](https://github.com/golang/groupcache) in Scala.

[![Build Status](https://api.travis-ci.org/jmconrad/scala-groupcache.png)](http://travis-ci.org/jmconrad/scala-groupcache)

The current version is 0.5.0, which is built against Scala 2.10.2.

If you are using sbt, you can pull this library down from the Sonatype OSS repository by adding the following line to your build:

```scala
libraryDependencies += "org.groupcache" %% "scala-groupcache" % "0.5.0"
```

## Basic Usage

```scala
import scala.concurrent.future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import java.net.URL
import groupcache._
import groupcache.peers.http.{HttpPeer, HttpPeerPicker}
import groupcache.Implicits._

object BasicUsage extends App {
  // URL of the peer running in the current process.
  val myUrl = "http://localhost:8080"

  // URLs of all peers that will participate in the cache.
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
  // process is the owner of the key (or the value is in the current
  // process' hot cache), the value will be fetched locally.  Otherwise,
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
}
```

## Building

scala-groupcache is built using sbt 0.12.3.

If you are using IntelliJ, you can use the sbt-idea plugin to generate the project files.  Additionally, you will need
to configure the IntelliJ project to mark the target/scala-2.10/src_managed/scala folder as a Source Folder rather than
an Excluded Folder.  This is because the build uses [sbt-scalabuff](https://github.com/sbt/sbt-scalabuff) to generate
Scala code from the [groupcache protobuf](https://github.com/golang/groupcache/blob/master/groupcachepb/groupcache.proto)
definition, and the code that is generated under src_managed/ is excluded from source control.


## Differences from the original Go implementation

In most cases scala-groupcache tries to stick pretty closely to the behavior of the Go implementation.  The primary
area in which that is not the case is with potentially expensive operations that block.  Rather than following suit with
the Go implementation that blocks on HTTP requests and cache filling operations, scala-groupcache instead adopts a
non-blocking approach using Scala Futures.


## Dependencies

- [ScalaBuff](https://github.com/SandroGrzicic/ScalaBuff) - Used to generate Scala case classes from the groupcache protobuf definition.
- [Finagle](https://github.com/twitter/finagle) - Used for fetching and serving cache values over HTTP in a non-blocking way.
