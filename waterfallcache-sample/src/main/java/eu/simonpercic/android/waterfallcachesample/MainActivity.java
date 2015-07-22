package eu.simonpercic.android.waterfallcachesample;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;

import eu.simonpercic.android.waterfallcache.WaterfallCache;
import eu.simonpercic.android.waterfallcache.cache.Cache;
import eu.simonpercic.android.waterfallcache.expire.LazyExpirableCache;
import rx.Observable;


public class MainActivity extends ActionBarActivity implements OnClickListener {

    private static final String DEFAULT_KEY = "test";

    private Cache waterfallCache;
    private TextView tvValueDisplay;
    private EditText etValueInput;
    private EditText etKeyInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvValueDisplay = (TextView) findViewById(R.id.tv_value_display);
        etValueInput = (EditText) findViewById(R.id.et_value_input);
        etKeyInput = (EditText) findViewById(R.id.et_key_input);
        etKeyInput.setText(DEFAULT_KEY);

        findViewById(R.id.btn_get_value).setOnClickListener(this);
        findViewById(R.id.btn_put_value).setOnClickListener(this);
        findViewById(R.id.btn_remove_value).setOnClickListener(this);
        findViewById(R.id.btn_contains_value).setOnClickListener(this);
        findViewById(R.id.btn_clear).setOnClickListener(this);

        Cache cache = WaterfallCache.Builder.create()
                .addMemoryCache(1000)
                .addDiskCache(this, 1024 * 1024)
                .build();

        waterfallCache = LazyExpirableCache.fromCache(cache, 10, TimeUnit.SECONDS);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override public void onClick(View v) {
        int id = v.getId();

        switch (id) {
            case R.id.btn_get_value:
                getTest();
                break;
            case R.id.btn_put_value:
                putTest();
                break;
            case R.id.btn_remove_value:
                removeTest();
                break;
            case R.id.btn_contains_value:
                containsTest();
                break;
            case R.id.btn_clear:
                clearTest();
                break;
        }
    }

    private void getTest() {
        Observable<String> test = waterfallCache.get(getKey(), String.class);
        test.subscribe(tvValueDisplay::setText, throwable -> showOnErrorMessage("Get", throwable));
    }

    private void putTest() {
        if (TextUtils.isEmpty(etValueInput.getText())) {
            return;
        }

        waterfallCache.put(getKey(), etValueInput.getText().toString()).subscribe(
                success -> showOnNextSuccessMessage("Put", success),
                throwable -> showOnErrorMessage("Put", throwable));
    }

    private void removeTest() {
        waterfallCache.remove(getKey()).subscribe(
                success -> showOnNextSuccessMessage("Remove", success),
                throwable -> showOnErrorMessage("Remove", throwable));
    }

    private void containsTest() {
        waterfallCache.contains(getKey()).subscribe(
                contains -> showMessage(String.format("Contains: %s", contains)),
                throwable -> showOnErrorMessage("Contains", throwable));
    }

    private void clearTest() {
        waterfallCache.clear().subscribe(
                success -> showOnNextSuccessMessage("Clear", success),
                throwable -> showOnErrorMessage("Clear", throwable));
    }

    private String getKey() {
        String key = etKeyInput.getText().toString();

        if (TextUtils.isEmpty(key)) {
            return DEFAULT_KEY;
        }

        return key;
    }

    private void showOnNextSuccessMessage(String tag, boolean success) {
        showMessage(String.format("%s: %s", tag, success ? "success" : "failed"));
    }

    private void showOnErrorMessage(String tag, Throwable throwable) {
        showMessage(String.format("%s error: %s", tag, throwable.getMessage()));
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
