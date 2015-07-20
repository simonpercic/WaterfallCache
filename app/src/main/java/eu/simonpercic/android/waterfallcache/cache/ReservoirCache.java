package eu.simonpercic.android.waterfallcache.cache;

import android.content.Context;

import com.anupcowkur.reservoir.Reservoir;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observable.Transformer;
import rx.Subscriber;
import rx.schedulers.Schedulers;

/**
 * Created by Simon Percic on 18/07/15.
 */
public class ReservoirCache implements Cache {

    public ReservoirCache(Context context, int sizeInBytes) {
        try {
            Reservoir.init(context.getApplicationContext(), sizeInBytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override public <T> Observable<T> get(String key, Class<T> classOfT) {
        return Reservoir.getAsync(key, classOfT).compose(applySchedulers())
                .onErrorReturn(throwable -> null);
    }

    @Override public Observable<Boolean> put(String key, Object object) {
        return Reservoir.putAsync(key, object).compose(applySchedulers());
    }

    @Override public Observable<Boolean> contains(String key) {
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

    @Override public Observable<Boolean> remove(String key) {
        return Reservoir.deleteAsync(key).compose(applySchedulers());
    }

    @Override public Observable<Boolean> clear() {
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
