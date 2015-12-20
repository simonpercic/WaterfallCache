package com.github.simonpercic.waterfallcache.callback;

/**
 * WaterfallCache get callback.
 *
 * @author Simon Percic <a href="https://github.com/simonpercic">https://github.com/simonpercic</a>
 */
public interface WaterfallGetCallback<T> extends WaterfallFailureCallback {

    /**
     * Called on success. Returns the value.
     *
     * @param object returned object
     */
    void onSuccess(T object);
}
