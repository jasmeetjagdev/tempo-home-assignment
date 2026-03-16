## Code Review

You are reviewing the following code submitted as part of a task to implement an item cache in a highly concurrent application. The anticipated load includes: thousands of reads per second, hundreds of writes per second, tens of concurrent threads.
Your objective is to identify and explain the issues in the implementation that must be addressed before deploying the code to production. Please provide a clear explanation of each issue and its potential impact on production behaviour.

```kotlin
import java.util.concurrent.ConcurrentHashMap

class SimpleCache<K, V> {
    private val cache = ConcurrentHashMap<K, CacheEntry<V>>()
    private val ttlMs = 60000 // 1 minute
    
    data class CacheEntry<V>(val value: V, val timestamp: Long)
    
    fun put(key: K, value: V) {
        cache[key] = CacheEntry(value, System.currentTimeMillis())
    }
    
    fun get(key: K): V? {
        val entry = cache[key]
        if (entry != null) {
            if (System.currentTimeMillis() - entry.timestamp < ttlMs) {
                return entry.value
            }
        }
        return null
    }
    
    fun size(): Int {
        return cache.size
    }
}
```

# Issues identified in the implementation:

## 1. Cache keeps growing as there is no cleanup happening on reaching ttl
- There is no cleanup code written that will clean the cache when the ttl is reached. This is a potential memory leak and will end up filling the memory after some time in Prod.
- There should be code written to delete the entry on ttl. That will save the memory leak.

## 2. The entry is retrieved from cache in line 20 at T0 and the corresponding value is returned later at T4 after checking two conditions (at T2 and T3). 
By that time, theoretically it is possible that value retrieved becomes stale due to two reasons as follows: 
Reasons: a. Either some other thread might have written to that same key
         b. Or the ttl has expired and we still return the value after the ttl expiry. 
            (This is very less likely, but contention and CPU throttling might cause this)
			
In summary, there is a gap between the time of retrieval and the time of return of the value and the operation is not done atomically so the results can be inconsistent.

## 3. You may end up returning null
This being a multi threaded system, there is a possibility that we end up returning null from one thread, while the other thread would have set the value
just in the mean time before our current thread returns the value. In short, this also ties to point #2 that there is gap between the time of retrieval and return that 
causes the values to be inconsistent.

## 4. Distributed system may have duplicate keys. 
If we use various pods in systems like kubernetes deployment and this cache is part of an API call or Kafka consumer which is load balanced,
then this cache will be native to each pod. It is possible that more than one pod stores the same key with a different value.
So depending on whichever pod picks up the request, the corresponding value could be returned. This is more a design cosideration and not an issue with this code explicitly.

## 5. Incorrect size of the cache
Because we are never deleting the entries from the cache, the size of the cache always grows. 
Even if the entries expire, still the size increases and the value returned by the size method becomes incorrect when entries start expiring. 

