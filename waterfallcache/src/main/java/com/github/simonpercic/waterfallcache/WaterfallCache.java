package com.github.simonpercic.waterfallcache;

import android.content.Context;
import android.util.LruCache;

import com.github.simonpercic.waterfallcache.cache.Cache;
import com.github.simonpercic.waterfallcache.cache.ObservableMemoryLruCache;
import com.github.simonpercic.waterfallcache.cache.ReservoirCache;
import com.github.simonpercic.waterfallcache.util.ObserverUtil;

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
public class WaterfallCache implements Cache {

    // cache levels
    private final List<Cache> caches;

    // inline memory cache, separate to cache levels for performance's sake
    private final LruCache<String, Object> memoryCache;

    // observe on Scheduler
    private Scheduler observeOnScheduler;

    private WaterfallCache(List<Cache> caches, int inlineMemoryCacheSize) {
        this.caches = caches;
        this.observeOnScheduler = AndroidSchedulers.mainThread();

        if (inlineMemoryCacheSize > 0) {
            this.memoryCache = new LruCache<>(inlineMemoryCacheSize);
        } else {
            this.memoryCache = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Observable<T> get(final String key, final Class<T> classOfT) {
        if (memoryCache != null) {
            T memoryValue = classOfT.cast(memoryCache.get(key));

            if (memoryValue != null) {
                return Observable.just(memoryValue).compose(applySchedulers());
            }
        }

        return achieveOnce(null, cache -> cache.get(key, classOfT), value -> value != null)
                .map(resultWrapper -> {
                    if (resultWrapper.result != null && resultWrapper.hitCacheIdx > 0) {
                        Observable<Boolean> observable = Observable.just(false);

                        for (int i = 0; i < resultWrapper.hitCacheIdx; i++) {
                            Cache cache = caches.get(i);

                            observable = observable.flatMap(success -> cache.put(key, resultWrapper.result));
                        }

                        observable.subscribe(ObserverUtil.silentObserver());
                    }

                    return resultWrapper.result;
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
                .map(resultWrapper -> {
                    if (resultWrapper.result && resultWrapper.hitCacheIdx > 0) {
                        get(key, Object.class).subscribe(ObserverUtil.silentObserver());
                    }

                    return resultWrapper.result;
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

        return doOnAll(Cache::clear);
    }

    /**
     * Performs a cache function on all cache levels sequentially.
     *
     * @param cacheFn cache function to perform on all cache levels
     * @return Observable that emits <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    private Observable<Boolean> doOnAll(Func1<Cache, Observable<Boolean>> cacheFn) {
        Observable<Boolean> observable = Observable.just(false);

        for (int i = 0; i < caches.size(); i++) {
            Cache cache = caches.get(i);

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
            Func1<Cache, Observable<T>> cacheFn,
            Predicate<T> condition) {

        Observable<T> observable = Observable.just(defaultValue);

        AtomicInteger hitIndex = new AtomicInteger();

        for (int i = 0; i < caches.size(); i++) {
            Cache cache = caches.get(i);

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

    /**
     * Sets a scheduler to observe on.
     *
     * @param scheduler Scheduler
     */
    @SuppressWarnings("NullableProblems")
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

    // region Builder

    /**
     * Cache builder
     */
    public static class Builder {
        private final List<Cache> caches;
        private int inlineMemoryCacheSize;

        private Builder() {
            caches = new ArrayList<>();
        }

        /**
         * Creates a new cache builder.
         *
         * @return cache builder
         */
        public static Builder create() {
            return new Builder();
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
         * Add a pre-defined observable memory cache to the cache levels. Use {#addMemoryCache} for performance.
         *
         * @param size max items to cache
         * @return Builder
         * @see ObservableMemoryLruCache
         */
        public Builder addObservableMemoryCache(int size) {
            return addCache(new ObservableMemoryLruCache(size));
        }

        /**
         * Add a pre-defined disk cache to the cache levels.
         *
         * @param context context
         * @param sizeInBytes max cache size in bytes
         * @return Builder
         * @see ReservoirCache
         */
        public Builder addDiskCache(Context context, int sizeInBytes) {
            return addCache(new ReservoirCache(context, sizeInBytes));
        }

        /**
         * Add a generic cache to the cache levels.
         *
         * @param cache cache
         * @return Builder
         */
        public Builder addCache(Cache cache) {
            caches.add(cache);
            return this;
        }

        /**
         * Builds the WaterfallCache.
         *
         * @return WaterfallCache
         */
        public WaterfallCache build() {
            return new WaterfallCache(caches, inlineMemoryCacheSize);
        }
    }

    // endregion
}
