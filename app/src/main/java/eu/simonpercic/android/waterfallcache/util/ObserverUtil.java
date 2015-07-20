package eu.simonpercic.android.waterfallcache.util;

import rx.Observer;

/**
 * Created by Simon Percic on 20/07/15.
 */
public final class ObserverUtil {
    private ObserverUtil() {
    }

    public static <T> Observer<T> silentObserver() {
        return new Observer<T>() {
            @Override public void onCompleted() {

            }

            @Override public void onError(Throwable e) {

            }

            @Override public void onNext(T t) {

            }
        };
    }
}
