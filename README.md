# WaterfallCache 

An Observable Android cache composed of multiple cache levels.

## How does it work?
### Read operations
When getting a value, it will first try to obtain it from the first cache level, if it does not contain it, it will try to obtain it from the next cache level. This continues through all cache levels until the value is obtained.

Upon obtaining the value, it is written to all cache levels that are lower than the level the value was obtained from.

### Write operations
Values are written and removed from all cache levels.

## Caches
Includes the following caches:

- memory cache, implemented by [LruCache](http://developer.android.com/reference/android/util/LruCache.html)
- disk cache, implemented by the awesome [Reservoir](https://github.com/anupcowkur/Reservoir) by [Anup Cowkur](https://github.com/anupcowkur)

You can also implement your own cache and add it to cache levels, as long as it implements the [Cache interface](waterfallcache/src/main/java/com/github/simonpercic/waterfallcache/cache/Cache.java).

## Values expiration
The library includes a LazyExpirableCache that can work with any [Cache](waterfallcache/src/main/java/com/github/simonpercic/waterfallcache/cache/Cache.java). It enables lazy value expiration based on the insertion time and an expiration time. Since it's lazy, the values are removed only when trying to obtain them.

## Usage

Add using Gradle:
```groovy
compile 'TODO'
``` 

```java
// create cache
Cache cache = WaterfallCache.Builder.create()
                .addMemoryCache(1000)
                .addDiskCache(this, 1024 * 1024)
                .build();
```

```java
// use the following Cache methods
Observable<T> get(String key, Class<T> classOfT);

Observable<Boolean> put(String key, Object object);

Observable<Boolean> contains(String key);

Observable<Boolean> remove(String key);

Observable<Boolean> clear();
```

```java
// create an expirable cache
Cache expirableCache = LazyExpirableCache.fromCache(cache, 10, TimeUnit.MINUTES);
```

## Sample application
See the included sample application to see a practical example of usage.

## License

Open source, distributed under the MIT License. See [LICENSE](LICENSE) for details.
