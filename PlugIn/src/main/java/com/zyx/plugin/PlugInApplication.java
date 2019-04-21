package com.zyx.plugin;

import android.content.Context;
import android.content.res.Resources;

public class PlugInApplication {

    private static Resources sResources;
    private static Context sApplicationBaseContext;

    public static void init(Context applicationBaseContext, Resources resources) {
        sApplicationBaseContext = applicationBaseContext;
        sResources = resources;
    }

    public static Resources getResources() {
        return sResources;
    }

    public static Context getApplicationBasecontext() {
        return sApplicationBaseContext;
    }
}
