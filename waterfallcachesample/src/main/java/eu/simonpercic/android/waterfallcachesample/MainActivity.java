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

import eu.simonpercic.android.waterfallcache.WaterfallCache;
import rx.Observable;


public class MainActivity extends ActionBarActivity implements OnClickListener {

    private WaterfallCache waterfallCache;
    private TextView tvValueDisplay;
    private EditText etValueInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvValueDisplay = (TextView) findViewById(R.id.tv_value_display);
        etValueInput = (EditText) findViewById(R.id.et_value_input);

        findViewById(R.id.btn_get_value).setOnClickListener(this);
        findViewById(R.id.btn_put_value).setOnClickListener(this);

        waterfallCache = WaterfallCache.create(this);
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
        }
    }

    private void getTest() {
        Observable<String> test = waterfallCache.get("test", String.class);
        test.subscribe(tvValueDisplay::setText);
    }

    private void putTest() {
        if (TextUtils.isEmpty(etValueInput.getText())) {
            return;
        }

        waterfallCache.put("test", etValueInput.getText().toString()).subscribe();
    }
}
