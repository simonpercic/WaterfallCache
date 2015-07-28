package eu.simonpercic.android.waterfallcache.util;

import rx.Observer;

/**
 * Observer utility class.
 * <p>
 * Created by Simon Percic on 20/07/15.
 */
public final class ObserverUtil {

    private ObserverUtil() {
    }

    /**
     * Returns a new silent observer that does not do anything.
     *
     * @param <T> type of observer
     * @return a new silent observer
     */
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