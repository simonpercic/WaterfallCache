package com.github.simonpercic.waterfallcachesample.test;

import android.content.Context;
import android.content.Intent;

import com.github.simonpercic.waterfallcache.cache.Cache;
import com.github.simonpercic.waterfallcache.callback.WaterfallCallback;
import com.github.simonpercic.waterfallcache.callback.WaterfallGetCallback;
import com.github.simonpercic.waterfallcachesample.test.base.BaseTestActivity;
import com.github.simonpercic.waterfallcachesample.test.base.SimpleObject;

/**
 * @author Simon Percic <a href="https://github.com/simonpercic">https://github.com/simonpercic</a>
 */
public class AsyncActivity extends BaseTestActivity {

    @Override protected void getTest(Cache waterfallCache, String key) {
        waterfallCache.getAsync(key, SimpleObject.class, new WaterfallGetCallback<SimpleObject>() {
            @Override public void onSuccess(SimpleObject object) {
                showValue(object);
            }

            @Override public void onFailure(Throwable throwable) {
                showErrorMessage("Get", throwable);
            }
        });
    }

    @Override protected void putTest(Cache waterfallCache, String value, String key) {
        waterfallCache.putAsync(key, new SimpleObject(value), new WaterfallCallback() {
            @Override public void onSuccess() {
                showSuccessMessage("Put", true);
            }

            @Override public void onFailure(Throwable throwable) {
                showErrorMessage("Put", throwable);
            }
        });
    }

    @Override protected void removeTest(Cache waterfallCache, String key) {
        waterfallCache.removeAsync(key, new WaterfallCallback() {
            @Override public void onSuccess() {
                showSuccessMessage("Remove", true);
            }

            @Override public void onFailure(Throwable throwable) {
                showErrorMessage("Remove", throwable);
            }
        });
    }

    @Override protected void containsTest(Cache waterfallCache, String key) {
        waterfallCache.containsAsync(key, new WaterfallGetCallback<Boolean>() {
            @Override public void onSuccess(Boolean contains) {
                showMessage(String.format("Contains: %s", contains));
            }

            @Override public void onFailure(Throwable throwable) {
                showErrorMessage("Contains", throwable);
            }
        });
    }

    @Override protected void clearTest(Cache waterfallCache) {
        waterfallCache.clearAsync(new WaterfallCallback() {
            @Override public void onSuccess() {
                showSuccessMessage("Clear", true);
            }

            @Override public void onFailure(Throwable throwable) {
                showErrorMessage("Clear", throwable);
            }
        });
    }

    public static Intent getIntent(Context context) {
        return new Intent(context, AsyncActivity.class);
    }
}
