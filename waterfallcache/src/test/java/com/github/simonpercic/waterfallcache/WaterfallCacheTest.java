package com.github.simonpercic.waterfallcache;

import com.github.simonpercic.waterfallcache.cache.Cache;
import com.github.simonpercic.waterfallcache.model.SimpleObject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import rx.Observable;
import rx.schedulers.Schedulers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Simon Percic <a href="https://github.com/simonpercic">https://github.com/simonpercic</a>
 */
public class WaterfallCacheTest {

    @Mock Cache cache1;
    @Mock Cache cache2;

    WaterfallCache waterfallCache;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        waterfallCache = WaterfallCache.builder()
                .addCache(cache1)
                .addCache(cache2)
                .withObserveOnScheduler(Schedulers.immediate())
                .build();
    }

    @Test
    public void testPut() throws Exception {
        String key = "TEST_KEY";
        String value = "TEST_VALUE";

        SimpleObject object = new SimpleObject(value);

        when(cache1.put(eq(key), eq(object))).thenReturn(Observable.just(true));
        when(cache2.put(eq(key), eq(object))).thenReturn(Observable.just(true));

        ObservableTestUtils.testObservable(waterfallCache.put(key, object), Assert::assertTrue);

        verify(cache1).put(eq(key), eq(object));
        verify(cache2).put(eq(key), eq(object));
    }

    @Test
    public void testGetLevel1() throws Exception {
        String key = "TEST_KEY";
        String value = "TEST_VALUE";

        SimpleObject object = new SimpleObject(value);

        when(cache1.get(eq(key), eq(SimpleObject.class))).thenReturn(Observable.just(object));

        Observable<SimpleObject> observable = waterfallCache.get(key, SimpleObject.class);
        ObservableTestUtils.testObservable(observable, simpleObject -> assertEquals(value, simpleObject.getValue()));

        verify(cache1).get(eq(key), eq(SimpleObject.class));
        verifyZeroInteractions(cache2);
    }

    @Test
    public void testGetLevel2() throws Exception {
        String key = "TEST_KEY";
        String value = "TEST_VALUE";

        SimpleObject object = new SimpleObject(value);

        when(cache1.get(eq(key), eq(SimpleObject.class))).thenReturn(Observable.just(null));
        when(cache2.get(eq(key), eq(SimpleObject.class))).thenReturn(Observable.just(object));

        when(cache1.put(eq(key), eq(object))).thenReturn(Observable.just(true));

        Observable<SimpleObject> observable = waterfallCache.get(key, SimpleObject.class);
        ObservableTestUtils.testObservable(observable, simpleObject -> assertEquals(value, simpleObject.getValue()));

        verify(cache1).put(eq(key), eq(object));

        verify(cache1).get(eq(key), eq(SimpleObject.class));
        verify(cache2).get(eq(key), eq(SimpleObject.class));
    }

    @Test
    public void testGetNoValue() throws Exception {
        String key = "TEST_KEY";

        when(cache1.get(eq(key), eq(SimpleObject.class))).thenReturn(Observable.just(null));
        when(cache2.get(eq(key), eq(SimpleObject.class))).thenReturn(Observable.just(null));

        Observable<SimpleObject> observable = waterfallCache.get(key, SimpleObject.class);
        ObservableTestUtils.testObservable(observable, Assert::assertNull, false);

        verify(cache1).get(eq(key), eq(SimpleObject.class));
        verify(cache2).get(eq(key), eq(SimpleObject.class));
    }

    @Test
    public void testContainsLevel1() throws Exception {
        String key = "TEST_KEY";

        when(cache1.contains(eq(key))).thenReturn(Observable.just(true));

        Observable<Boolean> observable = waterfallCache.contains(key);
        ObservableTestUtils.testObservable(observable, Assert::assertTrue);

        verify(cache1).contains(eq(key));
        verifyZeroInteractions(cache2);
    }

    @Test
    public void testContainsLevel2() throws Exception {
        String key = "TEST_KEY";
        String value = "TEST_VALUE";

        SimpleObject object = new SimpleObject(value);

        when(cache1.contains(eq(key))).thenReturn(Observable.just(false));
        when(cache2.contains(eq(key))).thenReturn(Observable.just(true));

        when(cache1.get(eq(key), eq(Object.class))).thenReturn(Observable.just(null));
        when(cache2.get(eq(key), eq(Object.class))).thenReturn(Observable.just(object));

        when(cache1.put(eq(key), eq(object))).thenReturn(Observable.just(true));

        Observable<Boolean> observable = waterfallCache.contains(key);
        ObservableTestUtils.testObservable(observable, Assert::assertTrue);

        verify(cache1).contains(eq(key));
        verify(cache2).contains(eq(key));

        verify(cache1).get(eq(key), eq(Object.class));
        verify(cache2).get(eq(key), eq(Object.class));

        verify(cache1).put(eq(key), eq(object));
        verify(cache2, never()).put(eq(key), eq(object));
    }

    @Test
    public void testContainsNoValue() throws Exception {
        String key = "TEST_KEY";

        when(cache1.contains(eq(key))).thenReturn(Observable.just(false));
        when(cache2.contains(eq(key))).thenReturn(Observable.just(false));

        Observable<Boolean> observable = waterfallCache.contains(key);
        ObservableTestUtils.testObservable(observable, Assert::assertFalse);

        verify(cache1).contains(eq(key));
        verify(cache2).contains(eq(key));
    }

    @Test
    public void testRemove() throws Exception {
        String key = "TEST_KEY";

        when(cache1.remove(eq(key))).thenReturn(Observable.just(true));
        when(cache2.remove(eq(key))).thenReturn(Observable.just(true));

        ObservableTestUtils.testObservable(waterfallCache.remove(key), Assert::assertTrue);

        verify(cache1).remove(eq(key));
        verify(cache2).remove(eq(key));
    }

    @Test
    public void testClear() throws Exception {
        when(cache1.clear()).thenReturn(Observable.just(true));
        when(cache2.clear()).thenReturn(Observable.just(true));

        ObservableTestUtils.testObservable(waterfallCache.clear(), Assert::assertTrue);

        verify(cache1).clear();
        verify(cache2).clear();
    }
}
