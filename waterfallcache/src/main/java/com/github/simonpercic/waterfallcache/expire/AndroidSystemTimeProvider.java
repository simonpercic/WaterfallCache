package com.github.simonpercic.waterfallcache.expire;

import android.os.SystemClock;

/**
 * @author Simon Percic <a href="https://github.com/simonpercic">https://github.com/simonpercic</a>
 */
public final class AndroidSystemTimeProvider implements SimpleTimeProvider {

    @Override public long currentTime() {
        return SystemClock.elapsedRealtime();
    }
}
