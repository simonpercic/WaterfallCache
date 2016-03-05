package com.github.simonpercic.waterfallcachesample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;

import com.github.simonpercic.waterfallcachesample.test.AsyncActivity;
import com.github.simonpercic.waterfallcachesample.test.RxActivity;

/**
 * @author Simon Percic <a href="https://github.com/simonpercic">https://github.com/simonpercic</a>
 */
public class IntroActivity extends AppCompatActivity implements OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        findViewById(R.id.btn_rx_test).setOnClickListener(this);
        findViewById(R.id.btn_async_test).setOnClickListener(this);
    }

    @Override public void onClick(View v) {
        int id = v.getId();

        switch (id) {
            case R.id.btn_rx_test:
                startActivity(RxActivity.getIntent(this));
                break;
            case R.id.btn_async_test:
                startActivity(AsyncActivity.getIntent(this));
                break;
            default:
                throw new IllegalArgumentException("onClick view id");
        }
    }
}
