package com.github.simonpercic.waterfallcache.callback;

/**
 * WaterfallCache status callback.
 *
 * @author Simon Percic <a href="https://github.com/simonpercic">https://github.com/simonpercic</a>
 */
public interface WaterfallCallback extends WaterfallFailureCallback {

    /**
     * Called on success.
     */
    void onSuccess();
}
