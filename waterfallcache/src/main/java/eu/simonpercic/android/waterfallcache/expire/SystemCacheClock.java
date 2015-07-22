package eu.simonpercic.android.waterfallcache.expire;

import android.os.SystemClock;

/**
 * Created by Simon Percic on 21/07/15.
 */
public final class SystemCacheClock {
    private SystemCacheClock() {
    }

    public static long getCurrentTime() {
        return SystemClock.elapsedRealtime();
    }
}
