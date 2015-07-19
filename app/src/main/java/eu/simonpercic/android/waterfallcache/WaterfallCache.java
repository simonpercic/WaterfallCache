package eu.simonpercic.android.waterfallcache;

import android.content.Context;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import eu.simonpercic.android.waterfallcache.cache.Cache;
import eu.simonpercic.android.waterfallcache.cache.MemoryLruCache;
import eu.simonpercic.android.waterfallcache.cache.ReservoirCache;
import rx.Observable;
import rx.Observable.Transformer;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by Simon Percic on 17/07/15.
 */
public class WaterfallCache implements Cache {

    @NonNull
    private final List<Cache> caches;

    private WaterfallCache(@NonNull List<Cache> caches) {
        this.caches = caches;
    }

    public <T> Observable<T> get(final String key, final Class<T> classOfT) {
        return achieveOnce(null, cache -> cache.get(key, classOfT), value -> value != null)
                .map(resultWrapper -> {
                    if (resultWrapper.result != null && resultWrapper.hitCacheIdx > 0) {
                        Observable<Boolean> observable = Observable.just(false);

                        for (int i = 0; i < resultWrapper.hitCacheIdx; i++) {
                            Cache cache = caches.get(i);

                            observable = observable.flatMap(success -> cache.put(key, resultWrapper.result));
                        }

                        observable.subscribe(getSilentObserver());
                    }

                    return resultWrapper;
                })
                .map(resultWrapper -> resultWrapper.result);
    }

    public Observable<Boolean> put(final String key, final Object object) {
        return doOnAll(cache -> cache.put(key, object));
    }

    public Observable<Boolean> contains(final String key) {
        return achieveOnce(false, cache -> cache.contains(key), value -> value)
                .map(resultWrapper -> {
                    if (resultWrapper.result && resultWrapper.hitCacheIdx > 0) {
                        get(key, Object.class).subscribe(getSilentObserver());
                    }

                    return resultWrapper;
                })
                .map(resultWrapper -> resultWrapper.result);
    }

    public Observable<Boolean> remove(final String key) {
        return doOnAll(cache -> cache.remove(key));
    }

    public Observable<Boolean> clear() {
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

    @SuppressWarnings("RedundantCast")
    private final Transformer schedulersTransformer = observable -> ((Observable) observable).observeOn(
            AndroidSchedulers.mainThread());

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

    private <T> Observer<T> getSilentObserver() {
        return new Observer<T>() {
            @Override public void onCompleted() {

            }

            @Override public void onError(Throwable e) {

            }

            @Override public void onNext(T t) {

            }
        };
    }

    // region Builder

    public static class Builder {
        private final List<Cache> caches;

        private Builder() {
            caches = new ArrayList<>();
        }

        public static Builder create() {
            return new Builder();
        }

        public Builder addMemoryCache(int size) {
            return addCache(new MemoryLruCache(size));
        }

        public Builder addDiskCache(Context context, int sizeInBytes) {
            return addCache(new ReservoirCache(context, sizeInBytes));
        }

        public Builder addCache(Cache cache) {
            caches.add(cache);
            return this;
        }

        public WaterfallCache build() {
            return new WaterfallCache(caches);
        }
    }

    // endregion
}
