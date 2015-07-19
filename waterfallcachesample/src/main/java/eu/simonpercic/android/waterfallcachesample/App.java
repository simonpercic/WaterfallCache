package eu.simonpercic.android.waterfallcachesample;

import android.app.Application;
import android.os.StrictMode;

/**
 * Created by Simon Percic on 18/07/15.
 */
public class App extends Application {

    @Override public void onCreate() {
        super.onCreate();
        setStrictMode();
    }

    private void setStrictMode() {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectAll()
                .penaltyLog()
                .build());

        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .penaltyDeath()
                .build());
    }
}
