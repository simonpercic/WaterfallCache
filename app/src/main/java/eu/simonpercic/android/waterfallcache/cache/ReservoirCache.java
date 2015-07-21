package eu.simonpercic.android.waterfallcache.cache;

import android.content.Context;
import android.support.annotation.NonNull;

import com.anupcowkur.reservoir.Reservoir;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observable.Transformer;
import rx.Subscriber;
import rx.schedulers.Schedulers;

/**
 * Reservoir disk cache.
 * Uses https://github.com/anupcowkur/Reservoir as underlying cache.
 * <p>
 * Created by Simon Percic on 18/07/15.
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
    @Override @NonNull
    public <T> Observable<T> get(@NonNull String key, @NonNull Class<T> classOfT) {
        return Reservoir.getAsync(key, classOfT).compose(applySchedulers())
                .onErrorReturn(throwable -> null);
    }

    /**
     * {@inheritDoc}
     */
    @Override @NonNull
    public Observable<Boolean> put(@NonNull String key, @NonNull Object object) {
        return Reservoir.putAsync(key, object).compose(applySchedulers());
    }

    /**
     * {@inheritDoc}
     */
    @Override @NonNull
    public Observable<Boolean> contains(@NonNull String key) {
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
    @Override @NonNull
    public Observable<Boolean> remove(@NonNull String key) {
        return Reservoir.deleteAsync(key).compose(applySchedulers());
    }

    /**
     * {@inheritDoc}
     */
    @Override @NonNull
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
