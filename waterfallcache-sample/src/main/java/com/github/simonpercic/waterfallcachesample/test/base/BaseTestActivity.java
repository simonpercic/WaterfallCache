package com.github.simonpercic.waterfallcachesample.test.base;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.simonpercic.waterfallcache.cache.Cache;
import com.github.simonpercic.waterfallcachesample.App;
import com.github.simonpercic.waterfallcachesample.R;

/**
 * @author Simon Percic <a href="https://github.com/simonpercic">https://github.com/simonpercic</a>
 */
public abstract class BaseTestActivity extends AppCompatActivity implements OnClickListener {

    private static final String DEFAULT_KEY = "test";

    private Cache waterfallCache;
    private TextView tvValueDisplay;
    private EditText etValueInput;
    private EditText etKeyInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        waterfallCache = ((App) getApplication()).getWaterfallCache();

        tvValueDisplay = (TextView) findViewById(R.id.tv_value_display);
        etValueInput = (EditText) findViewById(R.id.et_value_input);
        etKeyInput = (EditText) findViewById(R.id.et_key_input);
        etKeyInput.setText(DEFAULT_KEY);

        findViewById(R.id.btn_get_value).setOnClickListener(this);
        findViewById(R.id.btn_put_value).setOnClickListener(this);
        findViewById(R.id.btn_remove_value).setOnClickListener(this);
        findViewById(R.id.btn_contains_value).setOnClickListener(this);
        findViewById(R.id.btn_clear).setOnClickListener(this);
    }

    @Override public void onClick(View v) {
        int id = v.getId();

        switch (id) {
            case R.id.btn_get_value:
                getTest(waterfallCache, getKey());
                break;
            case R.id.btn_put_value:
                putTest();
                break;
            case R.id.btn_remove_value:
                removeTest(waterfallCache, getKey());
                break;
            case R.id.btn_contains_value:
                containsTest(waterfallCache, getKey());
                break;
            case R.id.btn_clear:
                clearTest(waterfallCache);
                break;
        }
    }

    protected abstract void getTest(Cache waterfallCache, String key);

    private void putTest() {
        if (TextUtils.isEmpty(etValueInput.getText())) {
            return;
        }

        putTest(waterfallCache, etValueInput.getText().toString(), getKey());
    }

    protected abstract void putTest(Cache waterfallCache, String value, String key);

    protected abstract void removeTest(Cache waterfallCache, String key);

    protected abstract void containsTest(Cache waterfallCache, String key);

    protected abstract void clearTest(Cache waterfallCache);

    protected void showValue(SimpleObject simpleObject) {
        tvValueDisplay.setText(simpleObject != null ? simpleObject.getValue() : "");
    }

    protected void showSuccessMessage(String tag, boolean success) {
        showMessage(String.format("%s: %s", tag, success ? "success" : "failed"));
    }

    protected void showErrorMessage(String tag, Throwable throwable) {
        showMessage(String.format("%s error: %s", tag, throwable.getMessage()));
    }

    protected void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private String getKey() {
        String key = etKeyInput.getText().toString();

        if (TextUtils.isEmpty(key)) {
            return DEFAULT_KEY;
        }

        return key;
    }
}
