package com.zyx.plugin;

import android.app.Activity;
import android.os.Bundle;

public class BaseActivity2 extends Activity {

    protected Activity that;

    public void setProxy(Activity proxyActivity) {
        that = proxyActivity;
    }

    @SuppressWarnings("all")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    }

    @Override
    public void setContentView(int layoutResID) {
        that.setContentView(layoutResID);
    }
}
