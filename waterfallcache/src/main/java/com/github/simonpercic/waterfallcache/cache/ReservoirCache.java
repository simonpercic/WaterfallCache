package com.github.simonpercic.waterfallcache.cache;

import android.content.Context;

import com.anupcowkur.reservoir.Reservoir;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observable.Transformer;
import rx.Subscriber;
import rx.schedulers.Schedulers;

/**
 * Reservoir disk cache.
 * Uses https://github.com/anupcowkur/Reservoir as underlying cache.
 *
 * @author Simon Percic <a href="https://github.com/simonpercic">https://github.com/simonpercic</a>
 */
public class ReservoirCache implements Cache {

    /**
     * Reservoir disk cache.
     *
     * @param context context
     * @param sizeInBytes max cache size in bytes
     */
    public ReservoirCache(Context context, int sizeInBytes) {
        try {
            Reservoir.init(context.getApplicationContext(), sizeInBytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Observable<T> get(String key, Class<T> classOfT) {
        return Reservoir.getAsync(key, classOfT).compose(applySchedulers())
                .onErrorReturn(throwable -> null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Observable<Boolean> put(String key, Object object) {
        return Reservoir.putAsync(key, object).compose(applySchedulers());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Observable<Boolean> contains(String key) {
        return Observable.create(new OnSubscribe<Boolean>() {
            @Override public void call(Subscriber<? super Boolean> subscriber) {
                try {
                    boolean contains = Reservoir.contains(key);
                    subscriber.onNext(contains);
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        }).compose(applySchedulers());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Observable<Boolean> remove(String key) {
        return Reservoir.deleteAsync(key).compose(applySchedulers());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Observable<Boolean> clear() {
        return Reservoir.clearAsync().compose(applySchedulers());
    }

    @SuppressWarnings("RedundantCast")
    private final Transformer schedulersTransformer = observable -> ((Observable) observable)
            .subscribeOn(Schedulers.io()).observeOn(Schedulers.immediate());

    private <T> Transformer<T, T> applySchedulers() {
        //noinspection unchecked
        return (Transformer<T, T>) schedulersTransformer;
    }
}
