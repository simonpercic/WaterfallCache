package com.github.simonpercic.waterfallcache.cache;

import android.util.LruCache;

import rx.Observable;
import rx.Observable.Transformer;
import rx.schedulers.Schedulers;

/**
 * Observable memory cache.
 *
 * @author Simon Percic <a href="https://github.com/simonpercic">https://github.com/simonpercic</a>
 */
public class ObservableMemoryLruCache implements Cache {

    // the underlying LruCache that holds the values
    private final LruCache<String, Object> lruCache;

    /**
     * Observable memory cache.
     *
     * @param size max items to cache
     */
    public ObservableMemoryLruCache(int size) {
        lruCache = new LruCache<>(size);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Observable<T> get(String key, Class<T> classOfT) {
        T value = classOfT.cast(lruCache.get(key));
        return Observable.just(value).compose(applySchedulers());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Observable<Boolean> put(String key, Object object) {
        lruCache.put(key, object);
        return successObservable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Observable<Boolean> contains(String key) {
        return get(key, Object.class).flatMap(o -> Observable.just(o != null).compose(applySchedulers()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Observable<Boolean> remove(String key) {
        lruCache.remove(key);
        return successObservable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Observable<Boolean> clear() {
        lruCache.evictAll();
        return successObservable();
    }

    private Observable<Boolean> successObservable() {
        return Observable.just(true).compose(applySchedulers());
    }

    @SuppressWarnings("RedundantCast")
    private final Transformer schedulersTransformer = observable -> ((Observable) observable).subscribeOn(
            Schedulers.immediate());

    private <T> Transformer<T, T> applySchedulers() {
        //noinspection unchecked
        return (Transformer<T, T>) schedulersTransformer;
    }
}
