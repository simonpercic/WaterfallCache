package com.github.simonpercic.waterfallcache.callback;

/**
 * WaterfallCache failure callback.
 *
 * @author Simon Percic <a href="https://github.com/simonpercic">https://github.com/simonpercic</a>
 */
public interface WaterfallFailureCallback {

    /**
     * Called on failure.
     *
     * @param throwable throwable
     */
    void onFailure(Throwable throwable);
}
