package com.lakeel.altla.sample.billing.item;

import android.app.Application;

import com.lakeel.altla.android.log.LogFactory;

public final class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        LogFactory.setDebug(BuildConfig.DEBUG);
    }
}
