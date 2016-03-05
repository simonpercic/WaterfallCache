package com.github.simonpercic.waterfallcachesample.test;

import android.content.Context;
import android.content.Intent;

import com.github.simonpercic.waterfallcache.cache.Cache;
import com.github.simonpercic.waterfallcachesample.test.base.BaseTestActivity;
import com.github.simonpercic.waterfallcachesample.test.base.SimpleObject;

import rx.Observable;

/**
 * @author Simon Percic <a href="https://github.com/simonpercic">https://github.com/simonpercic</a>
 */
public class RxActivity extends BaseTestActivity {

    @Override
    protected void getTest(Cache waterfallCache, String key) {
        Observable<SimpleObject> test = waterfallCache.get(key, SimpleObject.class);
        test.subscribe((simpleObject) -> {
            showValue(simpleObject);
        }, throwable -> showErrorMessage("Get", throwable));
    }

    @Override protected void putTest(Cache waterfallCache, String value, String key) {
        waterfallCache.put(key, new SimpleObject(value)).subscribe(
                success -> showSuccessMessage("Put", success),
                throwable -> showErrorMessage("Put", throwable));
    }

    @Override protected void removeTest(Cache waterfallCache, String key) {
        waterfallCache.remove(key).subscribe(
                success -> showSuccessMessage("Remove", success),
                throwable -> showErrorMessage("Remove", throwable));
    }

    @Override protected void containsTest(Cache waterfallCache, String key) {
        waterfallCache.contains(key).subscribe(
                contains -> showMessage(String.format("Contains: %s", contains)),
                throwable -> showErrorMessage("Contains", throwable));
    }

    @Override protected void clearTest(Cache waterfallCache) {
        waterfallCache.clear().subscribe(
                success -> showSuccessMessage("Clear", success),
                throwable -> showErrorMessage("Clear", throwable));
    }

    public static Intent getIntent(Context context) {
        return new Intent(context, RxActivity.class);
    }
}
