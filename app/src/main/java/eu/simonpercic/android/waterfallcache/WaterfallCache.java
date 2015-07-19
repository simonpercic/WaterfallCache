package eu.simonpercic.android.waterfallcache;

import android.content.Context;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import eu.simonpercic.android.waterfallcache.cache.Cache;
import eu.simonpercic.android.waterfallcache.cache.MemoryLruCache;
import eu.simonpercic.android.waterfallcache.cache.ReservoirCache;
import rx.Observable;
import rx.Observable.Transformer;
import rx.android.schedulers.AndroidSchedulers;
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
        Observable<T> observable = Observable.just((T) null);

        for (int i = 0; i < caches.size(); i++) {
            Cache cache = caches.get(i);

            if (i == 0) {
                observable = observable.flatMap(s -> cache.get(key, classOfT));
            } else {
                observable = observable.flatMap(o -> {
                    if (o != null) {
                        return Observable.just(o).subscribeOn(Schedulers.immediate());
                    } else {
                        return cache.get(key, classOfT);
                    }
                });
            }
        }

        return observable
                .onErrorReturn(throwable -> null)
                .compose(applySchedulers());
    }

    public Observable<Boolean> put(final String key, final Object object) {
        Observable<Boolean> observable = Observable.just(false);

        for (int i = 0; i < caches.size(); i++) {
            Cache cache = caches.get(i);

            observable = observable.flatMap(success -> cache.put(key, object));
        }

        return observable.compose(applySchedulers());
    }

    public Observable<Boolean> contains(final String key) {
        Observable<Boolean> observable = Observable.just(false);

        for (int i = 0; i < caches.size(); i++) {
            Cache cache = caches.get(i);

            if (i == 0) {
                observable = observable.flatMap(s -> cache.contains(key));
            } else {
                observable = observable.flatMap(contains -> {
                    if (contains) {
                        return Observable.just(true).subscribeOn(Schedulers.immediate());
                    } else {
                        return cache.contains(key);
                    }
                });
            }
        }

        return observable.compose(applySchedulers());
    }

    public Observable<Boolean> remove(final String key) {
        Observable<Boolean> observable = Observable.just(false);

        for (int i = 0; i < caches.size(); i++) {
            Cache cache = caches.get(i);

            observable = observable.flatMap(success -> cache.remove(key));
        }

        return observable.compose(applySchedulers());
    }

    public Observable<Boolean> clear() {
        Observable<Boolean> observable = Observable.just(false);

        for (int i = 0; i < caches.size(); i++) {
            Cache cache = caches.get(i);

            observable = observable.flatMap(success -> cache.clear());
        }

        return observable.compose(applySchedulers());
    }

    @SuppressWarnings("RedundantCast")
    private final Transformer schedulersTransformer = observable -> ((Observable) observable).observeOn(
            AndroidSchedulers.mainThread());

    private <T> Transformer<T, T> applySchedulers() {
        //noinspection unchecked
        return (Transformer<T, T>) schedulersTransformer;
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
            caches.add(new MemoryLruCache(size));
            return this;
        }

        public Builder addDiskCache(Context context, int sizeInBytes) {
            caches.add(new ReservoirCache(context, sizeInBytes));
            return this;
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
