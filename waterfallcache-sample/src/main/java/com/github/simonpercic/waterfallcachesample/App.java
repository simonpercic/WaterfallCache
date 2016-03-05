package com.github.simonpercic.waterfallcachesample;

import android.app.Application;
import android.os.StrictMode;

import com.github.simonpercic.waterfallcache.WaterfallCache;
import com.github.simonpercic.waterfallcache.cache.Cache;
import com.github.simonpercic.waterfallcache.expire.LazyExpirableCache;

import java.util.concurrent.TimeUnit;

/**
 * @author Simon Percic <a href="https://github.com/simonpercic">https://github.com/simonpercic</a>
 */
public class App extends Application {

    private Cache waterfallCache;

    @Override public void onCreate() {
        super.onCreate();
        setStrictMode();
        waterfallCache = createCache();
    }

    public Cache getWaterfallCache() {
        return waterfallCache;
    }

    private Cache createCache() {
        Cache cache = WaterfallCache.builder()
                .addMemoryCache(1000)
                .addDiskCache(this, 1024 * 1024)
                .build();

        return LazyExpirableCache.fromCache(cache, 10, TimeUnit.MINUTES);
    }

    private void setStrictMode() {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectAll()
                .penaltyLog()
                .build());

        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .penaltyDeath()
                .build());
    }
}
