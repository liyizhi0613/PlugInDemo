package com.zyx.plugindemo;

import android.app.Application;
import android.content.Context;

import com.zyx.plugindemo.Common.ActivityHookHelper;
import com.zyx.plugindemo.Common.PluginManager;
import com.zyx.plugindemo.Common.Utile;

public class HostApplication extends Application {
    private static Context sContext;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        sContext = base;

        // 这是测试代码，只为模拟下载插件过程
        Utile.simulationDownload(base);

        // 加载插件
        PluginManager.load(base);

        // Hook Activity
        ActivityHookHelper.hookActivity();
    }

    public static Context getContext() {
        return sContext;
    }
}
