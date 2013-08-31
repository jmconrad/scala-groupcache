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

/**
 * Enum of types of caches that can be maintained by a group.
 */
object CacheType extends Enumeration {
  type CacheType = Value

  // A main cache contains a cache of the keys/values for which the current peer
  // (process) is the owner.
  val MainCache,

  // A hot cache contains a cache of the keys/values for which the current
  // peer (process) is not the owner, but the keys are accessed frequently
  // enough that the data is mirrored here.
  HotCache = Value
}

