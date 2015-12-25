package com.github.simonpercic.waterfallcache;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import com.github.simonpercic.waterfallcache.cache.RxCache;
import com.github.simonpercic.waterfallcache.model.GenericObject;
import com.github.simonpercic.waterfallcache.model.SimpleObject;
import com.github.simonpercic.waterfallcache.model.WrappedObject;
import com.google.gson.reflect.TypeToken;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Simon Percic <a href="https://github.com/simonpercic">https://github.com/simonpercic</a>
 */
public class WaterfallCacheTest {

    private static WaterfallCache waterfallCache;

    private static RxCache mockCache;

    @BeforeClass
    public static void setUpClass() throws Exception {
        Context context = InstrumentationRegistry.getTargetContext();
        mockCache = mock(RxCache.class);

        waterfallCache = WaterfallCache.builder()
                .addDiskCache(context, 1024 * 1024)
                .addCache(mockCache)
                .withObserveOnScheduler(Schedulers.immediate())
                .build();
    }

    @Before
    public void setUp() throws Exception {
        reset(mockCache);

        when(mockCache.clear()).thenReturn(Observable.just(true));

        testObservable(waterfallCache.clear(), Assert::assertTrue);
    }

    @Test
    public void testContainsPrefetchSimpleObject() throws Exception {
        String key = "TEST_KEY";
        String value = "TEST_VALUE";

        SimpleObject object = new SimpleObject(value);

        when(mockCache.contains(eq(key))).thenReturn(Observable.just(true));

        when(mockCache.get(eq(key), eq(Object.class))).thenReturn(Observable.just(object));

        testObservable(waterfallCache.contains(key), Assert::assertTrue);

        Observable<SimpleObject> getObservable = waterfallCache.get(key, SimpleObject.class);
        testObservable(getObservable, result -> assertEquals(value, result.getValue()));

        verify(mockCache, never()).get(eq(key), eq(SimpleObject.class));
    }

    @Test
    public void testContainsPrefetchWrappedObject() throws Exception {
        String key = "TEST_KEY";
        String simpleValue = "TEST_SIMPLE_VALUE";
        String wrappedValue = "TEST_WRAPPED_VALUE";

        SimpleObject simpleObject = new SimpleObject(simpleValue);

        WrappedObject wrappedObject = new WrappedObject();
        wrappedObject.setObject(simpleObject);
        wrappedObject.setValue(wrappedValue);

        when(mockCache.contains(eq(key))).thenReturn(Observable.just(true));

        when(mockCache.get(eq(key), eq(Object.class))).thenReturn(Observable.just(wrappedObject));

        testObservable(waterfallCache.contains(key), Assert::assertTrue);

        Observable<WrappedObject> getObservable = waterfallCache.get(key, WrappedObject.class);
        testObservable(getObservable, result -> {
            assertEquals(wrappedValue, result.getValue());
            assertEquals(simpleValue, result.getObject().getValue());
        });

        verify(mockCache, never()).get(eq(key), eq(WrappedObject.class));
    }

    @Test
    public void testContainsPrefetchGenericObject() throws Exception {
        String key = "TEST_KEY";
        String simpleValue = "TEST_SIMPLE_VALUE";
        String wrappedValue = "TEST_WRAPPED_VALUE";
        String genericValue = "TEST_GENERIC_VALUE";

        SimpleObject simpleObject = new SimpleObject(simpleValue);

        WrappedObject wrappedObject = new WrappedObject();
        wrappedObject.setObject(simpleObject);
        wrappedObject.setValue(wrappedValue);

        GenericObject<WrappedObject> genericObject = new GenericObject<>();
        genericObject.setValue(genericValue);
        genericObject.setObject(wrappedObject);

        when(mockCache.contains(eq(key))).thenReturn(Observable.just(true));

        when(mockCache.get(eq(key), eq(Object.class))).thenReturn(Observable.just(genericObject));

        testObservable(waterfallCache.contains(key), Assert::assertTrue);

        Type type = new TypeToken<GenericObject<WrappedObject>>() {
        }.getType();

        Observable<GenericObject<WrappedObject>> getObservable = waterfallCache.get(key, type);
        testObservable(getObservable, result -> {
            assertEquals(genericValue, result.getValue());
            assertEquals(wrappedValue, result.getObject().getValue());
            assertEquals(simpleValue, result.getObject().getObject().getValue());
        });

        verify(mockCache, never()).get(eq(key), eq(type));
    }

    @Test
    public void testGetPrefetchSimpleObject() throws Exception {
        String key = "TEST_KEY";
        String value = "TEST_VALUE";

        SimpleObject object = new SimpleObject(value);

        when(mockCache.get(eq(key), eq(SimpleObject.class))).thenReturn(Observable.just(object));

        Observable<SimpleObject> observable = waterfallCache.get(key, SimpleObject.class);
        testObservable(observable, simpleObject -> assertEquals(value, simpleObject.getValue()));

        observable = waterfallCache.get(key, SimpleObject.class);
        testObservable(observable, result -> assertEquals(value, result.getValue()));

        verify(mockCache).get(eq(key), eq(SimpleObject.class));
    }

    @Test
    public void testGetPrefetchWrappedObject() throws Exception {
        String key = "TEST_KEY";
        String simpleValue = "TEST_SIMPLE_VALUE";
        String wrappedValue = "TEST_WRAPPED_VALUE";

        SimpleObject simpleObject = new SimpleObject(simpleValue);

        WrappedObject wrappedObject = new WrappedObject();
        wrappedObject.setObject(simpleObject);
        wrappedObject.setValue(wrappedValue);

        when(mockCache.get(eq(key), eq(WrappedObject.class))).thenReturn(Observable.just(wrappedObject));

        Observable<WrappedObject> observable = waterfallCache.get(key, WrappedObject.class);
        testObservable(observable, result -> {
            assertEquals(wrappedValue, result.getValue());
            assertEquals(simpleValue, result.getObject().getValue());
        });

        observable = waterfallCache.get(key, WrappedObject.class);
        testObservable(observable, result -> {
            assertEquals(wrappedValue, result.getValue());
            assertEquals(simpleValue, result.getObject().getValue());
        });

        verify(mockCache).get(eq(key), eq(WrappedObject.class));
    }

    @Test
    public void testGetPrefetchGenericObject() throws Exception {
        String key = "TEST_KEY";
        String simpleValue = "TEST_SIMPLE_VALUE";
        String wrappedValue = "TEST_WRAPPED_VALUE";
        String genericValue = "TEST_GENERIC_VALUE";

        SimpleObject simpleObject = new SimpleObject(simpleValue);

        WrappedObject wrappedObject = new WrappedObject();
        wrappedObject.setObject(simpleObject);
        wrappedObject.setValue(wrappedValue);

        GenericObject<WrappedObject> genericObject = new GenericObject<>();
        genericObject.setValue(genericValue);
        genericObject.setObject(wrappedObject);

        Type type = new TypeToken<GenericObject<WrappedObject>>() {
        }.getType();

        when(mockCache.get(eq(key), eq(type))).thenReturn(Observable.just(genericObject));

        Observable<GenericObject<WrappedObject>> getObservable = waterfallCache.get(key, type);
        testObservable(getObservable, result -> {
            assertEquals(genericValue, result.getValue());
            assertEquals(wrappedValue, result.getObject().getValue());
            assertEquals(simpleValue, result.getObject().getObject().getValue());
        });

        getObservable = waterfallCache.get(key, type);
        testObservable(getObservable, result -> {
            assertEquals(genericValue, result.getValue());
            assertEquals(wrappedValue, result.getObject().getValue());
            assertEquals(simpleValue, result.getObject().getObject().getValue());
        });

        verify(mockCache).get(eq(key), eq(type));
    }

    private <T> void testObservable(Observable<T> observable, Action1<T> assertAction) throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        observable.subscribeOn(Schedulers.immediate())
                .subscribe((value) -> {
                    if (assertAction != null) {
                        assertAction.call(value);
                    }
                    countDownLatch.countDown();
                }, throwable -> {
                    fail();
                });

        if (!countDownLatch.await(1000, TimeUnit.MILLISECONDS)) {
            fail();
        }
    }
}
