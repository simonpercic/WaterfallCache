package com.github.simonpercic.waterfallcache.expire;

import com.github.simonpercic.waterfallcache.ObservableTestUtils;
import com.github.simonpercic.waterfallcache.WaterfallCache;
import com.github.simonpercic.waterfallcache.cache.Cache;
import com.github.simonpercic.waterfallcache.expire.LazyExpirableCache.TimedValue;
import com.github.simonpercic.waterfallcache.model.SimpleObject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.schedulers.Schedulers;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Simon Percic <a href="https://github.com/simonpercic">https://github.com/simonpercic</a>
 */
public class LazyExpirableCacheTest {

    private WaterfallCache waterfallCache;

    @Mock Cache mockCache;
    @Mock SimpleTimeProvider simpleTimeProvider;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        waterfallCache = WaterfallCache.builder()
                .addMemoryCache(10)
                .addCache(mockCache)
                .withObserveOnScheduler(Schedulers.immediate())
                .build();
    }

    @Test
    public void testExpiredRemove() throws Exception {
        LazyExpirableCache lazyExpirableCache = LazyExpirableCache.fromCache(waterfallCache, 10, TimeUnit.SECONDS,
                simpleTimeProvider);

        long currentTime = TimeUnit.HOURS.toMillis(2);
        when(simpleTimeProvider.currentTime()).thenReturn(currentTime);

        String cacheKey = "TEST_KEY";
        String testValue = "TEST_VALUE";

        when(mockCache.get(eq(cacheKey), any()))
                .thenReturn(Observable.just(
                        new TimedValue<>(new SimpleObject(testValue), currentTime - TimeUnit.SECONDS.toMillis(15))));

        when(mockCache.remove(eq(cacheKey))).thenReturn(Observable.just(true));

        Observable<SimpleObject> observable = lazyExpirableCache.get(cacheKey, SimpleObject.class);

        ObservableTestUtils.testObservable(observable, Assert::assertNull, false);

        verify(mockCache).remove(eq(cacheKey));

        reset(mockCache);

        when(mockCache.get(eq(cacheKey), eq(SimpleObject.class))).thenReturn(Observable.just(null));

        observable = waterfallCache.get(cacheKey, SimpleObject.class);

        ObservableTestUtils.testObservable(observable, Assert::assertNull, false);
    }
}
