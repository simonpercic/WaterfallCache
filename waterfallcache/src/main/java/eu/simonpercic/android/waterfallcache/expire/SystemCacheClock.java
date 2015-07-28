package eu.simonpercic.android.waterfallcache.expire;

import android.os.SystemClock;

/**
 * @author Simon Percic <a href="https://github.com/simonpercic">https://github.com/simonpercic</a>
 */
public final class SystemCacheClock {
    private SystemCacheClock() {
    }

    public static long getCurrentTime() {
        return SystemClock.elapsedRealtime();
    }
}
