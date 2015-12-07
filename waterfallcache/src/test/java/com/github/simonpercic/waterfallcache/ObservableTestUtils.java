package com.github.simonpercic.waterfallcache;

import java.util.List;

import rx.Observable;
import rx.functions.Action1;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Simon Percic <a href="https://github.com/simonpercic">https://github.com/simonpercic</a>
 */
public final class ObservableTestUtils {

    private ObservableTestUtils() {
        //no instance
    }

    public static <T> void testObservable(Observable<T> observable, Action1<T> assertAction, boolean assertNotNull) {
        observable = observable.subscribeOn(Schedulers.immediate());

        TestSubscriber<T> testSubscriber = new TestSubscriber<>();
        observable.subscribe(testSubscriber);

        testSubscriber.assertNoErrors();
        testSubscriber.assertValueCount(1);

        List<T> onNextEvents = testSubscriber.getOnNextEvents();
        assertEquals(1, onNextEvents.size());

        T value = onNextEvents.get(0);

        if (assertNotNull) {
            assertNotNull(value);
        }

        assertAction.call(value);
    }

    public static <T> void testObservable(Observable<T> observable, Action1<T> assertAction) {
        testObservable(observable, assertAction, true);
    }
}
