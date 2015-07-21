package eu.simonpercic.android.waterfallcache.cache;

import android.support.annotation.NonNull;

import rx.Observable;

/**
 * Generic cache interface
 * <p>
 * Created by Simon Percic on 18/07/15.
 */
public interface Cache {
    /**
     * Get from cache.
     *
     * @param key key
     * @param classOfT class of cache value
     * @param <T> type of cache value
     * @return Observable that emits the cache value
     */
    @NonNull <T> Observable<T> get(@NonNull String key, @NonNull Class<T> classOfT);

    /**
     * Put value to cache.
     *
     * @param key key
     * @param object value
     * @return Observable that emits <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    @NonNull Observable<Boolean> put(@NonNull String key, @NonNull Object object);

    /**
     * Cache contains key.
     *
     * @param key key
     * @return Observable that emits <tt>true</tt> if cache contains key, <tt>false</tt> otherwise
     */
    @NonNull Observable<Boolean> contains(@NonNull String key);

    /**
     * Remove cache value.
     *
     * @param key key
     * @return Observable that emits <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    @NonNull Observable<Boolean> remove(@NonNull String key);

    /**
     * Clear all cache values
     *
     * @return Observable that emits <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    @NonNull Observable<Boolean> clear();
}
