package eu.simonpercic.android.waterfallcache.expire;

import android.support.annotation.NonNull;

import java.util.concurrent.TimeUnit;

import eu.simonpercic.android.waterfallcache.cache.Cache;
import eu.simonpercic.android.waterfallcache.util.ObserverUtil;
import rx.Observable;

/**
 * Lazily expirable cache.
 * Cache items expire after a set time.
 * Being lazy, items only expire when getting them from cache.
 * <p>
 * Created by Simon Percic on 20/07/15.
 */
public class LazyExpirableCache implements Cache {

    // the underlying cache that holds the values
    private final Cache underlyingCache;

    // expire after milliseconds
    private final long expireMillis;

    private LazyExpirableCache(Cache underlyingCache, long expireMillis) {
        this.underlyingCache = underlyingCache;
        this.expireMillis = expireMillis;
    }

    /**
     * Creates an lazy expirable cache from an actual Cache.
     *
     * @param cache the underlying cache that will hold the values
     * @param expireAfter expire after value
     * @param expireAfterUnit expire after time unit
     * @return lazy expirable cache instance
     */
    public static LazyExpirableCache fromCache(Cache cache, long expireAfter, TimeUnit expireAfterUnit) {
        long millis = expireAfterUnit.toMillis(expireAfter);
        return new LazyExpirableCache(cache, millis);
    }

    /**
     * {@inheritDoc}
     */
    @Override @NonNull
    public <T> Observable<T> get(@NonNull String key, @NonNull Class<T> classOfT) {
        return underlyingCache.get(key, TimedValue.class).map(timedValue -> {
            if (timedValue == null) {
                return null;
            }

            if (timedValue.addedOn + expireMillis < getCurrentTime()) {
                underlyingCache.remove(key).subscribe(ObserverUtil.silentObserver());
                return null;
            } else {
                return classOfT.cast(timedValue.value);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override @NonNull
    public Observable<Boolean> put(@NonNull String key, @NonNull Object object) {
        TimedValue timedValue = new TimedValue(object, getCurrentTime());
        return underlyingCache.put(key, timedValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override @NonNull
    public Observable<Boolean> contains(@NonNull String key) {
        return get(key, Object.class).flatMap(o -> Observable.just(o != null));
    }

    /**
     * {@inheritDoc}
     */
    @Override @NonNull
    public Observable<Boolean> remove(@NonNull String key) {
        return underlyingCache.remove(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override @NonNull
    public Observable<Boolean> clear() {
        return underlyingCache.clear();
    }

    private static long getCurrentTime() {
        return SystemCacheClock.getCurrentTime();
    }

    static class TimedValue {
        Object value;
        long addedOn;

        TimedValue(Object value, long time) {
            this.value = value;
            this.addedOn = time;
        }
    }
}
