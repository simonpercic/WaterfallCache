package com.github.simonpercic.waterfallcache.expire;

import com.github.simonpercic.waterfallcache.cache.Cache;
import com.github.simonpercic.waterfallcache.expire.LazyExpirableCache.TimedValue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.functions.Action1;
import rx.observers.TestSubscriber;

import static org.junit.Assert.assertEquals;
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

        when(underlyingCache.get(eq(cacheKey), eq(TimedValue.class)))
                .thenReturn(Observable.just(
                        new TimedValue(new TestCacheValue(testValue), currentTime - TimeUnit.SECONDS.toMillis(5))));

        //noinspection Convert2Lambda
        lazyExpirableCache.get(cacheKey, TestCacheValue.class).subscribe(new Action1<TestCacheValue>() {
            @Override public void call(TestCacheValue timedValue) {
                assertEquals(timedValue.value, testValue);
            }
        });
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

        when(underlyingCache.get(eq(cacheKey), eq(TimedValue.class)))
                .thenReturn(Observable.just(
                        new TimedValue(new TestCacheValue(testValue), currentTime - TimeUnit.SECONDS.toMillis(15))));

        when(underlyingCache.remove(eq(cacheKey))).thenReturn(Observable.just(true));

        TestSubscriber<TestCacheValue> testSubscriber = new TestSubscriber<>();
        lazyExpirableCache.get(cacheKey, TestCacheValue.class).subscribe(testSubscriber);
        testSubscriber.assertNoErrors();
        testSubscriber.assertValue(null);

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

        when(underlyingCache.get(eq(cacheKey), eq(TimedValue.class)))
                .thenReturn(Observable.just(null));

        TestSubscriber<TestCacheValue> testSubscriber = new TestSubscriber<>();
        lazyExpirableCache.get(cacheKey, TestCacheValue.class).subscribe(testSubscriber);
        testSubscriber.assertNoErrors();
        testSubscriber.assertValue(null);
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

        TestSubscriber<Boolean> testSubscriber = new TestSubscriber<>();
        lazyExpirableCache.put(cacheKey, testCacheValue).subscribe(testSubscriber);
        testSubscriber.assertNoErrors();
        testSubscriber.assertValue(true);

        TimedValue timedValue = timedValueArgumentCaptor.getValue();
        assertEquals(timedValue.value, testCacheValue);
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

        when(underlyingCache.get(eq(cacheKey), eq(TimedValue.class)))
                .thenReturn(Observable.just(
                        new TimedValue(new TestCacheValue(testValue), currentTime - TimeUnit.SECONDS.toMillis(5))));

        TestSubscriber<Boolean> testSubscriber = new TestSubscriber<>();
        lazyExpirableCache.contains(cacheKey).subscribe(testSubscriber);
        testSubscriber.assertNoErrors();
        testSubscriber.assertValue(true);
    }

    @Test
    public void testNotContains() throws Exception {
        Cache underlyingCache = mock(Cache.class);

        LazyExpirableCache lazyExpirableCache = LazyExpirableCache.fromCache(underlyingCache, 10, TimeUnit.SECONDS,
                simpleTimeProvider);

        long currentTime = TimeUnit.HOURS.toMillis(2);
        when(simpleTimeProvider.currentTime()).thenReturn(currentTime);

        String cacheKey = "cache_key";

        when(underlyingCache.get(eq(cacheKey), eq(TimedValue.class)))
                .thenReturn(Observable.just(null));

        TestSubscriber<Boolean> testSubscriber = new TestSubscriber<>();
        lazyExpirableCache.contains(cacheKey).subscribe(testSubscriber);
        testSubscriber.assertNoErrors();
        testSubscriber.assertValue(false);
    }

    @Test
    public void testRemove() throws Exception {
        Cache underlyingCache = mock(Cache.class);

        LazyExpirableCache lazyExpirableCache = LazyExpirableCache.fromCache(underlyingCache, 10, TimeUnit.SECONDS,
                simpleTimeProvider);

        String cacheKey = "cache_key";

        when(underlyingCache.remove(eq(cacheKey))).thenReturn(Observable.just(true));

        TestSubscriber<Boolean> testSubscriber = new TestSubscriber<>();
        lazyExpirableCache.remove(cacheKey).subscribe(testSubscriber);
        testSubscriber.assertNoErrors();
        testSubscriber.assertValue(true);

        verify(underlyingCache).remove(cacheKey);
    }

    @Test
    public void testClear() throws Exception {
        Cache underlyingCache = mock(Cache.class);

        LazyExpirableCache lazyExpirableCache = LazyExpirableCache.fromCache(underlyingCache, 10, TimeUnit.SECONDS,
                simpleTimeProvider);

        when(underlyingCache.clear()).thenReturn(Observable.just(true));

        TestSubscriber<Boolean> testSubscriber = new TestSubscriber<>();
        lazyExpirableCache.clear().subscribe(testSubscriber);
        testSubscriber.assertNoErrors();
        testSubscriber.assertValue(true);

        verify(underlyingCache).clear();
    }

    private static class TestCacheValue {
        private final String value;

        public TestCacheValue(String value) {
            this.value = value;
        }
    }
}
