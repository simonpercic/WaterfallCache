package com.github.simonpercic.waterfallcache.expire;

import com.github.simonpercic.waterfallcache.cache.Cache;
import com.github.simonpercic.waterfallcache.expire.LazyExpirableCache.TimedValue;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.concurrent.TimeUnit;

import rx.Observable;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

/**
 * LazyExpirableCache tests
 *
 * @author Simon Percic <a href="https://github.com/simonpercic">https://github.com/simonpercic</a>
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(SystemCacheClock.class)
public class LazyExpirableCacheTest {

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mockStatic(SystemCacheClock.class);
    }

    @Test
    public void testGetNotExpired() throws Exception {
        Cache underlyingCache = mock(Cache.class);

        LazyExpirableCache lazyExpirableCache = LazyExpirableCache.fromCache(underlyingCache, 10, TimeUnit.SECONDS);

        long currentTime = TimeUnit.HOURS.toMillis(2);
        when(SystemCacheClock.getCurrentTime()).thenReturn(currentTime);

        String cacheKey = "cache_key";
        String testValue = "test";

        when(underlyingCache.get(eq(cacheKey), eq(TimedValue.class)))
                .thenReturn(Observable.just(
                        new TimedValue(new TestCacheValue(testValue), currentTime - TimeUnit.SECONDS.toMillis(5))));

        lazyExpirableCache.get(cacheKey, TestCacheValue.class).subscribe(timedValue -> {
            assertEquals(timedValue.value, testValue);
        });
    }

    @Test
    public void testGetLazyExpired() throws Exception {
        Cache underlyingCache = mock(Cache.class);

        LazyExpirableCache lazyExpirableCache = LazyExpirableCache.fromCache(underlyingCache, 10, TimeUnit.SECONDS);

        long currentTime = TimeUnit.HOURS.toMillis(2);
        when(SystemCacheClock.getCurrentTime()).thenReturn(currentTime);

        String cacheKey = "cache_key";
        String testValue = "test";

        when(underlyingCache.get(eq(cacheKey), eq(TimedValue.class)))
                .thenReturn(Observable.just(
                        new TimedValue(new TestCacheValue(testValue), currentTime - TimeUnit.SECONDS.toMillis(15))));

        when(underlyingCache.remove(eq(cacheKey))).thenReturn(Observable.just(true));

        lazyExpirableCache.get(cacheKey, TestCacheValue.class).subscribe(Assert::assertNull);

        verify(underlyingCache).remove(cacheKey);
    }

    @Test
    public void testGetAlreadyExpired() throws Exception {
        Cache underlyingCache = mock(Cache.class);

        LazyExpirableCache lazyExpirableCache = LazyExpirableCache.fromCache(underlyingCache, 10, TimeUnit.SECONDS);

        long currentTime = TimeUnit.HOURS.toMillis(2);
        when(SystemCacheClock.getCurrentTime()).thenReturn(currentTime);

        String cacheKey = "cache_key";

        when(underlyingCache.get(eq(cacheKey), eq(TimedValue.class)))
                .thenReturn(Observable.just(null));

        lazyExpirableCache.get(cacheKey, TestCacheValue.class).subscribe(Assert::assertNull);
    }

    @Test
    public void testPut() throws Exception {
        Cache underlyingCache = mock(Cache.class);

        LazyExpirableCache lazyExpirableCache = LazyExpirableCache.fromCache(underlyingCache, 10, TimeUnit.SECONDS);

        long currentTime = TimeUnit.HOURS.toMillis(2);
        when(SystemCacheClock.getCurrentTime()).thenReturn(currentTime);

        String cacheKey = "cache_key";
        String testValue = "test";
        TestCacheValue testCacheValue = new TestCacheValue(testValue);

        ArgumentCaptor<TimedValue> timedValueArgumentCaptor = ArgumentCaptor.forClass(TimedValue.class);
        when(underlyingCache.put(eq(cacheKey), timedValueArgumentCaptor.capture())).thenReturn(Observable.just(true));

        lazyExpirableCache.put(cacheKey, testCacheValue).subscribe(Assert::assertTrue);

        TimedValue timedValue = timedValueArgumentCaptor.getValue();
        assertEquals(timedValue.value, testCacheValue);
        assertEquals(currentTime, timedValue.addedOn);
    }

    @Test
    public void testContains() throws Exception {
        Cache underlyingCache = mock(Cache.class);

        LazyExpirableCache lazyExpirableCache = LazyExpirableCache.fromCache(underlyingCache, 10, TimeUnit.SECONDS);

        long currentTime = TimeUnit.HOURS.toMillis(2);
        when(SystemCacheClock.getCurrentTime()).thenReturn(currentTime);

        String cacheKey = "cache_key";
        String testValue = "test";

        when(underlyingCache.get(eq(cacheKey), eq(TimedValue.class)))
                .thenReturn(Observable.just(
                        new TimedValue(new TestCacheValue(testValue), currentTime - TimeUnit.SECONDS.toMillis(5))));

        lazyExpirableCache.contains(cacheKey).subscribe(Assert::assertTrue);
    }

    @Test
    public void testNotContains() throws Exception {
        Cache underlyingCache = mock(Cache.class);

        LazyExpirableCache lazyExpirableCache = LazyExpirableCache.fromCache(underlyingCache, 10, TimeUnit.SECONDS);

        long currentTime = TimeUnit.HOURS.toMillis(2);
        when(SystemCacheClock.getCurrentTime()).thenReturn(currentTime);

        String cacheKey = "cache_key";

        when(underlyingCache.get(eq(cacheKey), eq(TimedValue.class)))
                .thenReturn(Observable.just(null));

        lazyExpirableCache.contains(cacheKey).subscribe(Assert::assertFalse);
    }

    @Test
    public void testRemove() throws Exception {
        Cache underlyingCache = mock(Cache.class);

        LazyExpirableCache lazyExpirableCache = LazyExpirableCache.fromCache(underlyingCache, 10, TimeUnit.SECONDS);

        String cacheKey = "cache_key";

        when(underlyingCache.remove(eq(cacheKey))).thenReturn(Observable.just(true));

        lazyExpirableCache.remove(cacheKey).subscribe(Assert::assertTrue);

        verify(underlyingCache).remove(cacheKey);
    }

    @Test
    public void testClear() throws Exception {
        Cache underlyingCache = mock(Cache.class);

        LazyExpirableCache lazyExpirableCache = LazyExpirableCache.fromCache(underlyingCache, 10, TimeUnit.SECONDS);

        when(underlyingCache.clear()).thenReturn(Observable.just(true));

        lazyExpirableCache.clear().subscribe(Assert::assertTrue);

        verify(underlyingCache).clear();
    }

    private static class TestCacheValue {
        private final String value;

        public TestCacheValue(String value) {
            this.value = value;
        }
    }
}
