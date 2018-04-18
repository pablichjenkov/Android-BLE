package com.letmeaccess.ble.serverapp;

import android.app.Application;
import com.letmeaccess.ble.serverapp.ci.LmaCrashManagerListener;
import net.hockeyapp.android.CrashManager;


public class ServerApp extends Application {


    private static final String HockeyAppSdkAppId = "e56484f42c1245b9ac365124e3ea36c4";


    @Override
    public void onCreate() {
        super.onCreate();

        CrashManager.register(this, HockeyAppSdkAppId, new LmaCrashManagerListener());
    }

}
