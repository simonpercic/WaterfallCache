package com.github.simonpercic.waterfallcache.expire;

import com.github.simonpercic.waterfallcache.ObservableTestUtils;
import com.github.simonpercic.waterfallcache.cache.Cache;
import com.github.simonpercic.waterfallcache.expire.LazyExpirableCache.TimedValue;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeUnit;

import rx.Observable;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * LazyExpirableCache tests
 *
 * @author Simon Percic <a href="https://github.com/simonpercic">https://github.com/simonpercic</a>
 */
public class LazyExpirableCacheTest {

    @Mock SimpleTimeProvider simpleTimeProvider;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetNotExpired() throws Exception {
        Cache underlyingCache = mock(Cache.class);

        LazyExpirableCache lazyExpirableCache = LazyExpirableCache.fromCache(underlyingCache, 10, TimeUnit.SECONDS,
                simpleTimeProvider);

        long currentTime = TimeUnit.HOURS.toMillis(2);
        when(simpleTimeProvider.currentTime()).thenReturn(currentTime);

        String cacheKey = "cache_key";
        String testValue = "test";

        when(underlyingCache.get(eq(cacheKey), any()))
                .thenReturn(Observable.just(
                        new TimedValue<>(new TestCacheValue(testValue), currentTime - TimeUnit.SECONDS.toMillis(5))));

        Observable<TestCacheValue> observable = lazyExpirableCache.get(cacheKey, TestCacheValue.class);
        ObservableTestUtils.testObservable(observable, testCacheValue -> assertEquals(testValue, testCacheValue.value));
    }

    @Test
    public void testGetLazyExpired() throws Exception {
        Cache underlyingCache = mock(Cache.class);

        LazyExpirableCache lazyExpirableCache = LazyExpirableCache.fromCache(underlyingCache, 10, TimeUnit.SECONDS,
                simpleTimeProvider);

        long currentTime = TimeUnit.HOURS.toMillis(2);
        when(simpleTimeProvider.currentTime()).thenReturn(currentTime);

        String cacheKey = "cache_key";
        String testValue = "test";

        when(underlyingCache.get(eq(cacheKey), any()))
                .thenReturn(Observable.just(
                        new TimedValue<>(new TestCacheValue(testValue), currentTime - TimeUnit.SECONDS.toMillis(15))));

        when(underlyingCache.remove(eq(cacheKey))).thenReturn(Observable.just(true));

        Observable<TestCacheValue> observable = lazyExpirableCache.get(cacheKey, TestCacheValue.class);
        ObservableTestUtils.testObservable(observable, Assert::assertNull, false);

        verify(underlyingCache).remove(cacheKey);
    }

    @Test
    public void testGetAlreadyExpired() throws Exception {
        Cache underlyingCache = mock(Cache.class);

        LazyExpirableCache lazyExpirableCache = LazyExpirableCache.fromCache(underlyingCache, 10, TimeUnit.SECONDS,
                simpleTimeProvider);

        long currentTime = TimeUnit.HOURS.toMillis(2);
        when(simpleTimeProvider.currentTime()).thenReturn(currentTime);

        String cacheKey = "cache_key";

        when(underlyingCache.get(eq(cacheKey), any()))
                .thenReturn(Observable.just(null));

        Observable<TestCacheValue> observable = lazyExpirableCache.get(cacheKey, TestCacheValue.class);
        ObservableTestUtils.testObservable(observable, Assert::assertNull, false);
    }

    @Test
    public void testPut() throws Exception {
        Cache underlyingCache = mock(Cache.class);

        LazyExpirableCache lazyExpirableCache = LazyExpirableCache.fromCache(underlyingCache, 10, TimeUnit.SECONDS,
                simpleTimeProvider);

        long currentTime = TimeUnit.HOURS.toMillis(2);
        when(simpleTimeProvider.currentTime()).thenReturn(currentTime);

        String cacheKey = "cache_key";
        String testValue = "test";
        TestCacheValue testCacheValue = new TestCacheValue(testValue);

        ArgumentCaptor<TimedValue> timedValueArgumentCaptor = ArgumentCaptor.forClass(TimedValue.class);
        when(underlyingCache.put(eq(cacheKey), timedValueArgumentCaptor.capture())).thenReturn(Observable.just(true));

        Observable<Boolean> observable = lazyExpirableCache.put(cacheKey, testCacheValue);
        ObservableTestUtils.testObservable(observable, Assert::assertTrue);

        TimedValue timedValue = timedValueArgumentCaptor.getValue();
        assertEquals(testCacheValue, timedValue.value);
        assertEquals(currentTime, timedValue.addedOn);
    }

    @Test
    public void testContains() throws Exception {
        Cache underlyingCache = mock(Cache.class);

        LazyExpirableCache lazyExpirableCache = LazyExpirableCache.fromCache(underlyingCache, 10, TimeUnit.SECONDS,
                simpleTimeProvider);

        long currentTime = TimeUnit.HOURS.toMillis(2);
        when(simpleTimeProvider.currentTime()).thenReturn(currentTime);

        String cacheKey = "cache_key";
        String testValue = "test";

        when(underlyingCache.get(eq(cacheKey), any()))
                .thenReturn(Observable.just(
                        new TimedValue<>(new TestCacheValue(testValue), currentTime - TimeUnit.SECONDS.toMillis(5))));

        Observable<Boolean> observable = lazyExpirableCache.contains(cacheKey);
        ObservableTestUtils.testObservable(observable, Assert::assertTrue);
    }

    @Test
    public void testNotContains() throws Exception {
        Cache underlyingCache = mock(Cache.class);

        LazyExpirableCache lazyExpirableCache = LazyExpirableCache.fromCache(underlyingCache, 10, TimeUnit.SECONDS,
                simpleTimeProvider);

        long currentTime = TimeUnit.HOURS.toMillis(2);
        when(simpleTimeProvider.currentTime()).thenReturn(currentTime);

        String cacheKey = "cache_key";

        when(underlyingCache.get(eq(cacheKey), any()))
                .thenReturn(Observable.just(null));

        Observable<Boolean> observable = lazyExpirableCache.contains(cacheKey);
        ObservableTestUtils.testObservable(observable, Assert::assertFalse);
    }

    @Test
    public void testRemove() throws Exception {
        Cache underlyingCache = mock(Cache.class);

        LazyExpirableCache lazyExpirableCache = LazyExpirableCache.fromCache(underlyingCache, 10, TimeUnit.SECONDS,
                simpleTimeProvider);

        String cacheKey = "cache_key";

        when(underlyingCache.remove(eq(cacheKey))).thenReturn(Observable.just(true));

        Observable<Boolean> observable = lazyExpirableCache.remove(cacheKey);
        ObservableTestUtils.testObservable(observable, Assert::assertTrue);

        verify(underlyingCache).remove(cacheKey);
    }

    @Test
    public void testClear() throws Exception {
        Cache underlyingCache = mock(Cache.class);

        LazyExpirableCache lazyExpirableCache = LazyExpirableCache.fromCache(underlyingCache, 10, TimeUnit.SECONDS,
                simpleTimeProvider);

        when(underlyingCache.clear()).thenReturn(Observable.just(true));

        Observable<Boolean> observable = lazyExpirableCache.clear();
        ObservableTestUtils.testObservable(observable, Assert::assertTrue);

        verify(underlyingCache).clear();
    }

    private static class TestCacheValue {
        private final String value;

        public TestCacheValue(String value) {
            this.value = value;
        }
    }
}
