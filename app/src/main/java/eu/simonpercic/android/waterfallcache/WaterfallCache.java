package eu.simonpercic.android.waterfallcache;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.LruCache;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import eu.simonpercic.android.waterfallcache.cache.Cache;
import eu.simonpercic.android.waterfallcache.cache.ObservableMemoryLruCache;
import eu.simonpercic.android.waterfallcache.cache.ReservoirCache;
import eu.simonpercic.android.waterfallcache.util.ObserverUtil;
import rx.Observable;
import rx.Observable.Transformer;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by Simon Percic on 17/07/15.
 */
public class WaterfallCache implements Cache {

    @NonNull
    private final List<Cache> caches;

    @Nullable
    private final LruCache<String, Object> memoryCache;

    @NonNull
    private Scheduler observeOnScheduler;

    private WaterfallCache(@NonNull List<Cache> caches, int inlineMemoryCacheSize) {
        this.caches = caches;
        this.observeOnScheduler = AndroidSchedulers.mainThread();

        if (inlineMemoryCacheSize > 0) {
            this.memoryCache = new LruCache<>(inlineMemoryCacheSize);
        } else {
            this.memoryCache = null;
        }
    }

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

    @Override
    public Observable<Boolean> put(final String key, final Object object) {
        if (memoryCache != null) {
            memoryCache.put(key, object);
        }

        return doOnAll(cache -> cache.put(key, object));
    }

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

    @Override
    public Observable<Boolean> remove(final String key) {
        if (memoryCache != null) {
            memoryCache.remove(key);
        }

        return doOnAll(cache -> cache.remove(key));
    }

    @Override
    public Observable<Boolean> clear() {
        if (memoryCache != null) {
            memoryCache.evictAll();
        }

        return doOnAll(Cache::clear);
    }

    private Observable<Boolean> doOnAll(Func1<Cache, Observable<Boolean>> cacheFn) {
        Observable<Boolean> observable = Observable.just(false);

        for (int i = 0; i < caches.size(); i++) {
            Cache cache = caches.get(i);

            observable = observable.flatMap(success -> cacheFn.call(cache));
        }

        return observable.compose(applySchedulers());
    }

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

    public static class Builder {
        private final List<Cache> caches;
        private int inlineMemoryCacheSize;

        private Builder() {
            caches = new ArrayList<>();
        }

        public static Builder create() {
            return new Builder();
        }

        public Builder addMemoryCache(int size) {
            inlineMemoryCacheSize = size;
            return this;
        }

        public Builder addObservableMemoryCache(int size) {
            return addCache(new ObservableMemoryLruCache(size));
        }

        public Builder addDiskCache(Context context, int sizeInBytes) {
            return addCache(new ReservoirCache(context, sizeInBytes));
        }

        public Builder addCache(Cache cache) {
            caches.add(cache);
            return this;
        }

        public WaterfallCache build() {
            return new WaterfallCache(caches, inlineMemoryCacheSize);
        }
    }

    // endregion
}
