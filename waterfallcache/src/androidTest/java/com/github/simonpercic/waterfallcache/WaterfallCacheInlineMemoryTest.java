package com.github.simonpercic.waterfallcache;

import com.github.simonpercic.waterfallcache.cache.Cache;
import com.github.simonpercic.waterfallcache.model.GenericObject;
import com.github.simonpercic.waterfallcache.model.SimpleObject;
import com.github.simonpercic.waterfallcache.model.WrappedObject;
import com.google.gson.reflect.TypeToken;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Type;

import rx.Observable;
import rx.schedulers.Schedulers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Simon Percic <a href="https://github.com/simonpercic">https://github.com/simonpercic</a>
 */
public class WaterfallCacheInlineMemoryTest {

    @Mock Cache cache;

    WaterfallCache waterfallCache;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        waterfallCache = WaterfallCache.builder()
                .addMemoryCache(100)
                .addCache(cache)
                .withObserveOnScheduler(Schedulers.immediate())
                .build();
    }

    @Test
    public void testGetNoValue() throws Exception {
        String key = "TEST_KEY";

        when(cache.get(eq(key), eq(SimpleObject.class))).thenReturn(Observable.just(null));

        Observable<SimpleObject> observable = waterfallCache.get(key, SimpleObject.class);

        ObservableTestUtils.testObservable(observable, Assert::assertNull, false);
    }

    @Test
    public void testContainsNoValue() throws Exception {
        String key = "TEST_KEY";

        when(cache.contains(eq(key))).thenReturn(Observable.just(false));

        Observable<Boolean> observable = waterfallCache.contains(key);
        ObservableTestUtils.testObservable(observable, Assert::assertFalse);
    }

    @Test
    public void testSimple() throws Exception {
        String key = "TEST_KEY";
        String value = "TEST_VALUE";

        SimpleObject object = new SimpleObject(value);

        when(cache.put(eq(key), eq(object))).thenReturn(Observable.just(true));

        ObservableTestUtils.testObservable(waterfallCache.put(key, object), Assert::assertTrue);

        verify(cache).put(eq(key), eq(object));

        Observable<SimpleObject> getObservable = waterfallCache.get(key, SimpleObject.class);
        ObservableTestUtils.testObservable(getObservable, simpleObject -> assertEquals(value, simpleObject.getValue()));

        ObservableTestUtils.testObservable(waterfallCache.contains(key), Assert::assertTrue);

        verifyNoMoreInteractions(cache);
    }

    @Test
    public void testWrapped() throws Exception {
        String key = "TEST_KEY";
        String simpleValue = "TEST_SIMPLE_VALUE";
        String wrappedValue = "TEST_WRAPPED_VALUE";

        SimpleObject simple = new SimpleObject(simpleValue);

        WrappedObject wrapped = new WrappedObject();
        wrapped.setObject(simple);
        wrapped.setValue(wrappedValue);

        when(cache.put(eq(key), eq(wrapped))).thenReturn(Observable.just(true));

        ObservableTestUtils.testObservable(waterfallCache.put(key, wrapped), Assert::assertTrue);

        verify(cache).put(eq(key), eq(wrapped));

        Observable<WrappedObject> getObservable = waterfallCache.get(key, WrappedObject.class);
        ObservableTestUtils.testObservable(getObservable, wrappedObject -> {
            assertEquals(wrappedValue, wrappedObject.getValue());
            assertEquals(simpleValue, wrappedObject.getObject().getValue());
        });

        ObservableTestUtils.testObservable(waterfallCache.contains(key), Assert::assertTrue);

        verifyNoMoreInteractions(cache);
    }

    @Test
    public void testGeneric() throws Exception {
        String key = "TEST_KEY";
        String simpleValue = "TEST_SIMPLE_VALUE";
        String wrappedValue = "TEST_WRAPPED_VALUE";
        String genericValue = "TEST_GENERIC_VALUE";

        SimpleObject simple = new SimpleObject(simpleValue);

        WrappedObject wrapped = new WrappedObject();
        wrapped.setObject(simple);
        wrapped.setValue(wrappedValue);

        GenericObject<WrappedObject> generic = new GenericObject<>();
        generic.setObject(wrapped);
        generic.setValue(genericValue);

        when(cache.put(eq(key), eq(generic))).thenReturn(Observable.just(true));

        ObservableTestUtils.testObservable(waterfallCache.put(key, generic), Assert::assertTrue);

        verify(cache).put(eq(key), eq(generic));

        Type type = new TypeToken<GenericObject<WrappedObject>>() {
        }.getType();

        Observable<GenericObject<WrappedObject>> getObservable = waterfallCache.get(key, type);
        ObservableTestUtils.testObservable(getObservable, wrappedObjectGenericObject -> {
            assertEquals(genericValue, wrappedObjectGenericObject.getValue());
            assertEquals(wrappedValue, wrappedObjectGenericObject.getObject().getValue());
            assertEquals(simpleValue, wrappedObjectGenericObject.getObject().getObject().getValue());
        });

        ObservableTestUtils.testObservable(waterfallCache.contains(key), Assert::assertTrue);

        verifyNoMoreInteractions(cache);
    }

    @Test
    public void testRemove() throws Exception {
        String key = "TEST_KEY";

        String value = "TEST_VALUE";

        SimpleObject simple = new SimpleObject(value);

        when(cache.put(eq(key), eq(simple))).thenReturn(Observable.just(true));
        when(cache.remove(eq(key))).thenReturn(Observable.just(true));
        when(cache.contains(eq(key))).thenReturn(Observable.just(false));

        waterfallCache.put(key, simple).subscribeOn(Schedulers.immediate()).subscribe();

        ObservableTestUtils.testObservable(waterfallCache.remove(key), Assert::assertTrue);

        ObservableTestUtils.testObservable(waterfallCache.contains(key), Assert::assertFalse);
    }

    @Test
    public void testRemoveNoValue() throws Exception {
        String key = "TEST_KEY";

        when(cache.remove(eq(key))).thenReturn(Observable.just(true));
        when(cache.contains(eq(key))).thenReturn(Observable.just(false));

        ObservableTestUtils.testObservable(waterfallCache.remove(key), Assert::assertTrue);

        ObservableTestUtils.testObservable(waterfallCache.contains(key), Assert::assertFalse);
    }

    @Test
    public void testClear() throws Exception {
        String key = "TEST_KEY";

        String value = "TEST_VALUE";

        SimpleObject simple = new SimpleObject(value);

        when(cache.put(eq(key), eq(simple))).thenReturn(Observable.just(true));
        when(cache.clear()).thenReturn(Observable.just(true));
        when(cache.contains(eq(key))).thenReturn(Observable.just(false));

        waterfallCache.put(key, simple).subscribeOn(Schedulers.immediate()).subscribe();

        Observable<Boolean> observable = waterfallCache.clear();
        ObservableTestUtils.testObservable(observable, Assert::assertTrue);

        ObservableTestUtils.testObservable(waterfallCache.contains(key), Assert::assertFalse);
    }
}
