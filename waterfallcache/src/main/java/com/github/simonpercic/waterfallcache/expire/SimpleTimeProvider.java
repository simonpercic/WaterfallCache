package com.github.simonpercic.waterfallcache.expire;

/**
 * Simple time provider interface.
 *
 * @author Simon Percic <a href="https://github.com/simonpercic">https://github.com/simonpercic</a>
 */
public interface SimpleTimeProvider {

    /**
     * Should return current time.
     *
     * @return current time in milliseconds
     */
    long currentTime();
}
