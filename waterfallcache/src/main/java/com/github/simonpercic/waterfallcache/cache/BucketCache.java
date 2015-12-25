package com.github.simonpercic.waterfallcache.cache;

import android.content.Context;

import com.github.simonpercic.bucket.Bucket;

import java.io.IOException;
import java.lang.reflect.Type;

import rx.Observable;

/**
 * Bucket disk cache.
 * Uses https://github.com/simonpercic/Bucket as the underlying cache implementation.
 *
 * @author Simon Percic <a href="https://github.com/simonpercic">https://github.com/simonpercic</a>
 */
public class BucketCache implements RxCache {

    // Bucket disk cache
    private final Bucket bucket;

    /**
     * Bucket disk cache.
     *
     * @param context context
     * @param maxSizeBytes max size of cache in bytes
     * @throws IOException
     */
    public BucketCache(Context context, long maxSizeBytes) throws IOException {
        this.bucket = Bucket.builder(context, maxSizeBytes).build();
    }

    /**
     * {@inheritDoc}
     */
    @Override public <T> Observable<T> get(String key, Type typeOfT) {
        return bucket.getRx(key, typeOfT);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Observable<Boolean> put(String key, Object object) {
        return bucket.putRx(key, object);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Observable<Boolean> contains(String key) {
        return bucket.containsRx(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Observable<Boolean> remove(String key) {
        return bucket.removeRx(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Observable<Boolean> clear() {
        return bucket.clearRx();
    }
}
