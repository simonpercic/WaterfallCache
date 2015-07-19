package eu.simonpercic.android.waterfallcache.cache;

import android.util.LruCache;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observable.Transformer;
import rx.Subscriber;
import rx.schedulers.Schedulers;

/**
 * Created by Simon Percic on 18/07/15.
 */
public class MemoryLruCache implements Cache {
    private final LruCache<String, Object> lruCache;

    public MemoryLruCache(int size) {
        lruCache = new LruCache<>(size);
    }

    @Override public <T> Observable<T> get(String key, Class<T> classOfT) {
        T value = classOfT.cast(lruCache.get(key));
        return Observable.just(value).compose(applySchedulers());
    }

    @Override public Observable<Boolean> put(String key, Object object) {
        return Observable.create(new OnSubscribe<Boolean>() {
            @Override public void call(Subscriber<? super Boolean> subscriber) {
                lruCache.put(key, object);

                subscriber.onNext(true);
                subscriber.onCompleted();
            }
        }).compose(applySchedulers());
    }

    @Override public Observable<Boolean> contains(String key) {
        return get(key, Object.class).flatMap(o -> Observable.just(o != null).compose(applySchedulers()));
    }

    @Override public Observable<Boolean> remove(String key) {
        return Observable.create(new OnSubscribe<Boolean>() {
            @Override public void call(Subscriber<? super Boolean> subscriber) {
                lruCache.remove(key);

                subscriber.onNext(true);
                subscriber.onCompleted();
            }
        }).compose(applySchedulers());
    }

    @Override public Observable<Boolean> clear() {
        return Observable.create(new OnSubscribe<Boolean>() {
            @Override public void call(Subscriber<? super Boolean> subscriber) {
                lruCache.evictAll();

                subscriber.onNext(true);
                subscriber.onCompleted();
            }
        }).compose(applySchedulers());
    }

    @SuppressWarnings("RedundantCast")
    private final Transformer schedulersTransformer = observable -> ((Observable) observable).subscribeOn(
            Schedulers.immediate());

    private <T> Transformer<T, T> applySchedulers() {
        //noinspection unchecked
        return (Transformer<T, T>) schedulersTransformer;
    }
}
