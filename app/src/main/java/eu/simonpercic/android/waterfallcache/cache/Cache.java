package eu.simonpercic.android.waterfallcache.cache;

import rx.Observable;

/**
 * Created by Simon Percic on 18/07/15.
 */
public interface Cache {
    <T> Observable<T> get(String key, Class<T> classOfT);

    Observable<Boolean> put(String key, Object object);

    Observable<Boolean> contains(String key);

    Observable<Boolean> remove(String key);

    Observable<Boolean> clear();
}
