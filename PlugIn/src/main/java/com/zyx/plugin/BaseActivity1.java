package com.zyx.plugin;

import android.app.Activity;
import android.content.res.Resources;

public class BaseActivity1 extends Activity {

    @Override
    public Resources getResources() {
        if (PlugInApplication.getResources() != null) {
            return PlugInApplication.getResources();
        } else {
            return super.getResources();
        }
    }
}
