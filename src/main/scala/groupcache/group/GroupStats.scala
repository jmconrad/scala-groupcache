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

package groupcache.group

import java.util.concurrent.atomic.AtomicInteger

class GroupStats {
  val gets = new AtomicInteger(0)
  val cacheHits = new AtomicInteger(0)
  val peerLoads = new AtomicInteger(0)
  val peerErrors = new AtomicInteger(0)
  val loads = new AtomicInteger(0)
  val loadsDeduped = new AtomicInteger(0)
  val localLoads = new AtomicInteger(0)
  val localLoadErrs = new AtomicInteger(0)
  val serverRequests = new AtomicInteger(0)
}

