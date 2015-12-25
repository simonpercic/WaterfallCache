package com.github.simonpercic.waterfallcache.cache;

import com.github.simonpercic.waterfallcache.callback.WaterfallCallback;
import com.github.simonpercic.waterfallcache.callback.WaterfallGetCallback;

import java.lang.reflect.Type;

/**
 * @author Simon Percic <a href="https://github.com/simonpercic">https://github.com/simonpercic</a>
 */
public interface AsyncCache {

    /**
     * Get from cache - async, using a callback.
     *
     * @param key key
     * @param typeOfT type of cache value
     * @param callback callback that will be invoked to return the value
     * @param <T> T of cache value
     */
    <T> void getAsync(String key, Type typeOfT, WaterfallGetCallback<T> callback);

    /**
     * Put value to cache - async, using a callback.
     *
     * @param key key
     * @param object object
     * @param callback callback that will be invoked to report status
     */
    void putAsync(String key, Object object, WaterfallCallback callback);

    /**
     * Cache contains key - async, using a callback.
     *
     * @param key key
     * @param callback callback that will be invoked to report contains state
     */
    void containsAsync(String key, WaterfallGetCallback<Boolean> callback);

    /**
     * Remove cache value - async, using a callback.
     *
     * @param key key
     * @param callback callback that will be invoked to report status
     */
    void removeAsync(String key, WaterfallCallback callback);

    /**
     * Clear all cache values - async, using a callback.
     *
     * @param callback callback that will be invoked to report status
     */
    void clearAsync(WaterfallCallback callback);
}
