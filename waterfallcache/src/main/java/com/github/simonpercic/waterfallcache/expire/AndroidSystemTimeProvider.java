package com.github.simonpercic.waterfallcache.expire;

import android.os.SystemClock;

/**
 * Android system time provider.
 *
 * @author Simon Percic <a href="https://github.com/simonpercic">https://github.com/simonpercic</a>
 */
public final class AndroidSystemTimeProvider implements SimpleTimeProvider {

    /**
     * Returns Android time since boot in milliseconds.
     *
     * @return elapsed milliseconds since boot
     */
    @Override public long currentTime() {
        return SystemClock.elapsedRealtime();
    }
}
