package com.github.simonpercic.waterfallcache.utils;

import com.github.simonpercic.waterfallcache.callback.WaterfallCallback;
import com.github.simonpercic.waterfallcache.callback.WaterfallFailureCallback;
import com.github.simonpercic.waterfallcache.callback.WaterfallGetCallback;

import rx.Observable;
import rx.functions.Action1;

/**
 * @author Simon Percic <a href="https://github.com/simonpercic">https://github.com/simonpercic</a>
 */
public final class AsyncUtils {

    private AsyncUtils() {
        //no instance
    }

    public static void doAsync(Observable<Boolean> observable, final WaterfallCallback callback) {
        observable.subscribe(success -> {
            if (callback != null) {
                callback.onSuccess();
            }
        }, asyncOnError(callback));
    }

    public static <T> void doAsync(Observable<T> observable, final WaterfallGetCallback<T> callback) {
        observable.subscribe(value -> {
            if (callback != null) {
                callback.onSuccess(value);
            }
        }, asyncOnError(callback));
    }

    private static Action1<Throwable> asyncOnError(final WaterfallFailureCallback callback) {
        return throwable -> {
            if (callback != null) {
                callback.onFailure(throwable);
            }
        };
    }
}
