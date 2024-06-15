package com.koisk.videokiosk.ads;

import android.app.Application;
import android.content.Context;

class App extends Application {
    private static Context mContext;
    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        AdManager.Init(this);
    }
    public static Context getContext() {
        return mContext;
    }
}