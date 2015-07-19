package eu.simonpercic.android.waterfallcache;

import android.content.Context;

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

    private final Cache memoryCache;
    private final Cache diskCache;

    private WaterfallCache(Context context) {
        memoryCache = new MemoryLruCache(1000);
        diskCache = new ReservoirCache(context, 1024 * 1024);
    }

    public static WaterfallCache create(Context context) {
        return new WaterfallCache(context);
    }

    public <T> Observable<T> get(final String key, final Class<T> classOfT) {
        return Observable.just(key)
                .flatMap(s -> memoryCache.get(key, classOfT))
                .flatMap(o -> {
                    if (o != null) {
                        return Observable.just(o).subscribeOn(Schedulers.immediate());
                    } else {
                        return diskCache.get(key, classOfT);
                    }
                })
                .onErrorReturn(throwable -> null)
                .compose(applySchedulers());
    }

    public Observable<Boolean> put(final String key, final Object object) {
        return Observable.just(key)
                .flatMap(s -> memoryCache.put(key, object))
                .flatMap(b -> diskCache.put(key, object))
                .compose(applySchedulers());
    }

    public Observable<Boolean> contains(final String key) {
        return Observable.just(key)
                .flatMap(s -> memoryCache.contains(key))
                .flatMap(contains -> {
                    if (contains) {
                        return Observable.just(true).subscribeOn(Schedulers.immediate());
                    } else {
                        return diskCache.contains(key);
                    }
                })
                .compose(applySchedulers());
    }

    public Observable<Boolean> remove(final String key) {
        return Observable.just(key)
                .flatMap(s -> memoryCache.remove(key))
                .flatMap(success -> diskCache.remove(key))
                .compose(applySchedulers());
    }

    public Observable<Boolean> clear() {
        return Observable.just(null)
                .flatMap(o -> memoryCache.clear())
                .flatMap(success -> diskCache.clear())
                .compose(applySchedulers());
    }

    @SuppressWarnings("RedundantCast")
    private final Transformer schedulersTransformer = observable -> ((Observable) observable).observeOn(
            AndroidSchedulers.mainThread());

    private <T> Transformer<T, T> applySchedulers() {
        //noinspection unchecked
        return (Transformer<T, T>) schedulersTransformer;
    }
}
