package eu.simonpercic.android.waterfallcache.expire;

import android.os.SystemClock;

import java.util.concurrent.TimeUnit;

import eu.simonpercic.android.waterfallcache.cache.Cache;
import eu.simonpercic.android.waterfallcache.util.ObserverUtil;
import rx.Observable;
import rx.functions.Func1;

/**
 * Created by Simon Percic on 20/07/15.
 */
public class LazyExpirableCache implements Cache {
    private final Cache underlyingCache;
    private final long expireMillis;

    private LazyExpirableCache(Cache underlyingCache, long expireMillis) {
        this.underlyingCache = underlyingCache;
        this.expireMillis = expireMillis;
    }

    public static LazyExpirableCache fromCache(Cache cache, long expireAfter, TimeUnit expireAfterUnit) {
        long millis = expireAfterUnit.toMillis(expireAfter);
        return new LazyExpirableCache(cache, millis);
    }

    @Override public <T> Observable<T> get(String key, Class<T> classOfT) {
        return underlyingCache.get(key, TimedValue.class).map(new Func1<TimedValue, T>() {
            @Override public T call(TimedValue timedValue) {
                if (timedValue == null) {
                    return null;
                }

                if (timedValue.addedOn + expireMillis < SystemClock.elapsedRealtime()) {
                    underlyingCache.remove(key).subscribe(ObserverUtil.silentObserver());
                    return null;
                } else {
                    return classOfT.cast(timedValue.value);
                }
            }
        });
    }

    @Override public Observable<Boolean> put(String key, Object object) {
        TimedValue timedValue = new TimedValue(object);
        return underlyingCache.put(key, timedValue);
    }

    @Override public Observable<Boolean> contains(String key) {
        return underlyingCache.contains(key);
    }

    @Override public Observable<Boolean> remove(String key) {
        return underlyingCache.remove(key);
    }

    @Override public Observable<Boolean> clear() {
        return underlyingCache.clear();
    }

    private static class TimedValue {
        private Object value;
        private long addedOn;

        private TimedValue(Object value) {
            this.value = value;
            this.addedOn = SystemClock.elapsedRealtime();
        }
    }
}
