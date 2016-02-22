# WaterfallCache 

Multi-level RxJava-powered cache for Android.

Supports Rx and async methods for all operations.

[![Build Status](https://api.travis-ci.org/simonpercic/WaterfallCache.svg?branch=master)](https://travis-ci.org/simonpercic/WaterfallCache)
[ ![Download](https://api.bintray.com/packages/simonpercic/maven/waterfallcache/images/download.svg) ](https://bintray.com/simonpercic/maven/waterfallcache/_latestVersion)

## Caches
Includes the following caches:

- memory cache, implemented by [LruCache](http://developer.android.com/reference/android/util/LruCache.html)
- [Bucket](https://github.com/simonpercic/Bucket) disk cache

You can also implement your own cache and add it to cache levels, as long as it implements the [RxCache interface](waterfallcache/src/main/java/com/github/simonpercic/waterfallcache/cache/RxCache.java).

## How does it work?
### Read operations
When getting a value, it will first try to obtain it from the first cache level, if it does not contain it, it will try to obtain it from the next cache level. This continues through all cache levels until the value is obtained.

Upon obtaining the value, it is written to all cache levels that are lower than the level the value was obtained from.

### Write operations
Values are written and removed from all cache levels.

## Usage

Add using Gradle:
```groovy
compile 'com.github.simonpercic:waterfallcache:1.1.0'
```

Use:
```java
// create cache
Cache cache = WaterfallCache.builder()
                .addMemoryCache(1000)
                .addDiskCache(this, 1024 * 1024)
                .build();
```

### Rx
```java
Observable<T> get(String key, Class<T> classOfT);

Observable<Boolean> put(String key, Object object);

Observable<Boolean> contains(String key);

Observable<Boolean> remove(String key);

Observable<Boolean> clear();
```

or

### Async
```java
<T> void getAsync(String key, Type typeOfT, WaterfallGetCallback<T> callback);

void putAsync(String key, Object object, WaterfallCallback callback);

void containsAsync(String key, WaterfallGetCallback<Boolean> callback);

void removeAsync(String key, WaterfallCallback callback);

void clearAsync(WaterfallCallback callback);
```

## Values expiration
The library includes a LazyExpirableCache that can work with any [Cache](waterfallcache/src/main/java/com/github/simonpercic/waterfallcache/cache/Cache.java). It enables lazy value expiration based on the insertion time and an expiration time. Since it's lazy, the values are removed only when trying to obtain them.

```java
// create an expirable cache by passing an instance of a Cache
Cache expirableCache = LazyExpirableCache.fromCache(cache, 10, TimeUnit.MINUTES);
```

### Time provider
By default, LazyExpirableCache uses Android's built-in [SystemClock.elapsedRealtime()](https://developer.android.com/reference/android/os/SystemClock.html#elapsedRealtime()) as a time provider in order to determine whether a cache value should expire. You can also provide your own time provider, by passing either:

- an implementation of [SimpleTimeProvider](waterfallcache/src/main/java/com/github/simonpercic/waterfallcache/expire/SimpleTimeProvider.java)

or

- an ```Observable<Long>``` that emits the current time

#### RxTime
You can use [RxTime](https://github.com/simonpercic/RxTime) as a complementary library that provides the current UTC time from the internet to serve as your time provider. That way the values are ensured to expire correctly, even if you are caching them for a longer period of time (which can span device deep sleep or even device restarts).

## Sample application
See the included sample application to see a practical example of usage.

## Change Log
See [CHANGELOG.md](CHANGELOG.md)

## License
Open source, distributed under the MIT License. See [LICENSE](LICENSE) for details.
