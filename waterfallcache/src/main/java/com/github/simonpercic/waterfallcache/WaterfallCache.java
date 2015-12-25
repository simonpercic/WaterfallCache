package com.github.simonpercic.waterfallcache;

import android.content.Context;
import android.util.Log;
import android.util.LruCache;

import com.github.simonpercic.waterfallcache.cache.BucketCache;
import com.github.simonpercic.waterfallcache.cache.Cache;
import com.github.simonpercic.waterfallcache.cache.RxCache;
import com.github.simonpercic.waterfallcache.callback.WaterfallCallback;
import com.github.simonpercic.waterfallcache.callback.WaterfallGetCallback;
import com.github.simonpercic.waterfallcache.utils.AsyncUtils;
import com.github.simonpercic.waterfallcache.utils.StringUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable;
import rx.Observable.Transformer;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Waterfall cache.
 * Composed of cache levels, if level N does not contain a value, tries to get it from level N+1.
 * Writes cache value from level N+1 to level N, if N does not contain it.
 * Writes and deletes values from all levels on {#put} and {#remove}.
 *
 * @author Simon Percic <a href="https://github.com/simonpercic">https://github.com/simonpercic</a>
 */
public final class WaterfallCache implements Cache {

    // cache levels
    private final List<RxCache> caches;

    // inline memory cache, separate to cache levels for performance's sake
    private final LruCache<String, Object> memoryCache;

    // observe on Scheduler
    private Scheduler observeOnScheduler;

    private WaterfallCache(List<RxCache> caches, Scheduler observeOnScheduler, int inlineMemoryCacheSize) {
        this.caches = caches;
        this.observeOnScheduler = observeOnScheduler;

        if (inlineMemoryCacheSize > 0) {
            this.memoryCache = new LruCache<>(inlineMemoryCacheSize);
        } else {
            this.memoryCache = null;
        }
    }

    // region Cache methods

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Observable<T> get(final String key, final Type typeOfT) {
        if (memoryCache != null) {
            //noinspection unchecked
            T memoryValue = (T) memoryCache.get(key);

            if (memoryValue != null) {
                return Observable.just(memoryValue).compose(applySchedulers());
            }
        }

        return achieveOnce(null, cache -> cache.<T>get(key, typeOfT), value -> value != null)
                .flatMap(resultWrapper -> {
                    if (resultWrapper.result != null && resultWrapper.hitCacheIdx > 0) {
                        Observable<Boolean> observable = Observable.just(false);

                        for (int i = 0; i < resultWrapper.hitCacheIdx; i++) {
                            RxCache cache = caches.get(i);

                            observable = observable.flatMap(success -> cache.put(key, resultWrapper.result));
                        }

                        return observable.map(success -> resultWrapper.result);
                    }

                    return Observable.just(resultWrapper.result);
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Observable<Boolean> put(final String key, final Object object) {
        if (memoryCache != null) {
            memoryCache.put(key, object);
        }

        return doOnAll(cache -> cache.put(key, object));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Observable<Boolean> contains(final String key) {
        if (memoryCache != null) {
            Object memoryValue = memoryCache.get(key);

            if (memoryValue != null) {
                return Observable.just(true).compose(applySchedulers());
            }
        }

        return achieveOnce(false, cache -> cache.contains(key), value -> value)
                .flatMap(resultWrapper -> {
                    if (resultWrapper.result && resultWrapper.hitCacheIdx > 0) {
                        return get(key, Object.class).map(o -> true);
                    }

                    return Observable.just(resultWrapper.result);
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Observable<Boolean> remove(final String key) {
        if (memoryCache != null) {
            memoryCache.remove(key);
        }

        return doOnAll(cache -> cache.remove(key));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Observable<Boolean> clear() {
        if (memoryCache != null) {
            memoryCache.evictAll();
        }

        return doOnAll(RxCache::clear);
    }

    // endregion Cache methods

    /**
     * Performs a cache function on all cache levels sequentially.
     *
     * @param cacheFn cache function to perform on all cache levels
     * @return Observable that emits <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    private Observable<Boolean> doOnAll(Func1<RxCache, Observable<Boolean>> cacheFn) {
        Observable<Boolean> observable = Observable.just(true);

        for (int i = 0; i < caches.size(); i++) {
            RxCache cache = caches.get(i);

            observable = observable.flatMap(success -> cacheFn.call(cache));
        }

        return observable.compose(applySchedulers());
    }

    /**
     * Performs a cache function on each cache level sequentially, until one cache level fulfills the predicate.
     *
     * @param defaultValue default value to emit if unsuccessful
     * @param cacheFn cache function to perform
     * @param condition predicate condition
     * @param <T> type of value
     * @return Observable that emits the value
     */
    private <T> Observable<ResultWrapper<T>> achieveOnce(
            T defaultValue,
            Func1<RxCache, Observable<T>> cacheFn,
            Predicate<T> condition) {

        Observable<T> observable = Observable.just(defaultValue);

        AtomicInteger hitIndex = new AtomicInteger();

        for (int i = 0; i < caches.size(); i++) {
            RxCache cache = caches.get(i);

            if (i == 0) {
                observable = observable.flatMap(s -> cacheFn.call(cache));
            } else {
                observable = observable.flatMap(value -> {
                    if (condition.apply(value)) {
                        return Observable.just(value).subscribeOn(Schedulers.immediate());
                    } else {
                        hitIndex.incrementAndGet();
                        return cacheFn.call(cache);
                    }
                });
            }
        }

        return observable
                .map(t -> new ResultWrapper<>(t, hitIndex.get()))
                .compose(applySchedulers());
    }

    // region asynchronous methods

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void getAsync(String key, Type typeOfT, WaterfallGetCallback<T> callback) {
        checkGetArgs(key, typeOfT);

        Observable<T> get = get(key, typeOfT);
        AsyncUtils.doAsync(get, callback);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putAsync(String key, Object object, WaterfallCallback callback) {
        checkPutArgs(key, object);

        AsyncUtils.doAsync(put(key, object), callback);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void containsAsync(String key, WaterfallGetCallback<Boolean> callback) {
        checkKeyArg(key);

        AsyncUtils.doAsync(contains(key), callback);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAsync(String key, WaterfallCallback callback) {
        checkKeyArg(key);

        AsyncUtils.doAsync(remove(key), callback);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearAsync(WaterfallCallback callback) {
        AsyncUtils.doAsync(clear(), callback);
    }

    // endregion asynchronous methods

    /**
     * Sets a scheduler to observe on.
     *
     * @param scheduler Scheduler
     */
    public void setObserveOnScheduler(Scheduler scheduler) {
        if (scheduler != null) {
            observeOnScheduler = scheduler;
        }
    }

    @SuppressWarnings("RedundantCast")
    private final Transformer schedulersTransformer = observable -> ((Observable) observable).observeOn(
            observeOnScheduler);

    private <T> Transformer<T, T> applySchedulers() {
        //noinspection unchecked
        return (Transformer<T, T>) schedulersTransformer;
    }

    private interface Predicate<T> {
        boolean apply(T value);
    }

    private static class ResultWrapper<T> {
        private final T result;
        private final int hitCacheIdx;

        public ResultWrapper(T result, int hitCacheIdx) {
            this.result = result;
            this.hitCacheIdx = hitCacheIdx;
        }
    }

    // region args checks

    private static void checkGetArgs(String key, Type typeOfT) {
        checkStringArgumentEmpty(key, "key");
        checkObjectArgumentNull(typeOfT, "typeOfT");
    }

    private static void checkPutArgs(String key, Object object) {
        checkStringArgumentEmpty(key, "key");
        checkObjectArgumentNull(object, "object");
    }

    private static void checkKeyArg(String key) {
        checkStringArgumentEmpty(key, "key");
    }

    private static void checkStringArgumentEmpty(String value, String name) {
        if (StringUtils.isEmpty(value)) {
            throw new IllegalArgumentException(String.format("%s is null or empty", name));
        }
    }

    private static void checkObjectArgumentNull(Object value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(String.format("%s is null", name));
        }
    }

    // endregion args checks

    // region Builder

    /**
     * Creates a new cache builder.
     *
     * @return cache builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Cache builder.
     */
    public static final class Builder {

        private final List<RxCache> caches;
        private int inlineMemoryCacheSize;
        private Scheduler observeOnScheduler;

        private Builder() {
            caches = new ArrayList<>();
        }

        /**
         * Set a custom observeOn scheduler to control the thread that receives the updates.
         * Defaults to the Android main thread from AndroidSchedulers.mainThread()
         *
         * @param scheduler scheduler to receive the updates
         * @return Builder
         */
        public Builder withObserveOnScheduler(Scheduler scheduler) {
            this.observeOnScheduler = scheduler;
            return this;
        }

        /**
         * Add an inline memory cache to the cache levels. Should probably be called before adding other cache levels.
         *
         * @param size max items to cache
         * @return Builder
         */
        public Builder addMemoryCache(int size) {
            inlineMemoryCacheSize = size;
            return this;
        }

        /**
         * Add a pre-defined disk cache to the cache levels.
         *
         * @param context context
         * @param sizeInBytes max cache size in bytes
         * @return Builder
         * @see com.github.simonpercic.waterfallcache.cache.BucketCache
         */
        public Builder addDiskCache(Context context, int sizeInBytes) {
            BucketCache cache;

            try {
                cache = new BucketCache(context, sizeInBytes);
            } catch (IOException e) {
                Log.w(WaterfallCache.class.getSimpleName(), e.getMessage());
                return this;
            }

            return addCache(cache);
        }

        /**
         * Add a generic cache to the cache levels.
         *
         * @param cache cache
         * @return Builder
         */
        public Builder addCache(RxCache cache) {
            caches.add(cache);
            return this;
        }

        /**
         * Builds the WaterfallCache.
         *
         * @return WaterfallCache
         */
        public WaterfallCache build() {
            if (observeOnScheduler == null) {
                observeOnScheduler = AndroidSchedulers.mainThread();
            }

            return new WaterfallCache(caches, observeOnScheduler, inlineMemoryCacheSize);
        }
    }

    // endregion
}
