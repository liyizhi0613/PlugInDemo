package com.zyx.plugindemo.Common;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;

import com.zyx.plugindemo.HostApplication;
import com.zyx.plugindemo.SubActivity;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class ActivityHookHelper {
    private static final String EXTRA_TARGET_INTENT = "extra_target_intent";

    public static void hookActivity() {
        try {
            hookAMN();
            hookActivityThread();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 第一处Hook地方，Hook ActivityManagerNative中通过getDefault方法获得的对象，使其在调用startActivity时替换成替身Activity，以达到欺骗ActivityManagerSerfvice的目的
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     */
    private static void hookAMN() throws ClassNotFoundException, IllegalAccessException, NoSuchFieldException {

        Object gDefaultObj = null;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // 获取 ActivityManagerNative 的 gDefault
            Class activityManagerNativeClass = Class.forName("android.app.ActivityManagerNative");
            Field gDefaultField = activityManagerNativeClass.getDeclaredField("gDefault");
            gDefaultField.setAccessible(true);
            gDefaultObj = gDefaultField.get(null);
        } else {
            // 获取 ActivityManager 的 IActivityManagerSingleton
            Class activityManagerNativeClass = Class.forName("android.app.ActivityManager");
            Field gDefaultField = activityManagerNativeClass.getDeclaredField("IActivityManagerSingleton");
            gDefaultField.setAccessible(true);
            gDefaultObj = gDefaultField.get(null);
        }


        // 获取 gDefault 对应在 android.util.Singleton<T> 的单例对象 mInstance
        Class singletonClass = Class.forName("android.util.Singleton");
        Field mInstanceField = singletonClass.getDeclaredField("mInstance");
        mInstanceField.setAccessible(true);
        final Object mInstanceObj = mInstanceField.get(gDefaultObj);

        // 创建 gDefault 的代理
        Class<?> classInterface = Class.forName("android.app.IActivityManager");
        Object proxy = Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class<?>[] { classInterface },
                new InvocationHandler() {

                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                        // 只 Hook startActivity 一个方法
                        if (!"startActivity".equals(method.getName())) {
                            return method.invoke(mInstanceObj, args);
                        }
                        // 找到参数里面的Intent 对象
                        int index = 0;
                        for (int i = 0; i < args.length; i++) {
                            if (args[i] instanceof Intent) {
                                index = i;
                                break;
                            }
                        }
                        Intent targetIntent = (Intent) args[index];

                        // 判断宿主没有声明的Activity才走替换流程
                        boolean isExistActivity = isExistActivity(targetIntent.getComponent().getClassName());
                        if (isExistActivity) {
                            return method.invoke(mInstanceObj, args);
                        }

                        String stubPackage = targetIntent.getComponent().getPackageName();

                        Intent newIntent = new Intent();
                        newIntent.setComponent(new ComponentName(stubPackage, SubActivity.class.getName()));

                        // 把原来要启动的目标Activity先存起来
                        newIntent.putExtra(EXTRA_TARGET_INTENT, targetIntent);

                        // 替换掉Intent, 即欺骗AMS要启动的是替身Activity
                        args[index] = newIntent;

                        return method.invoke(mInstanceObj, args);
                    }
                }
        );

        //把 gDefault 的 mInstance 字段，替换成 proxy
        mInstanceField.set(gDefaultObj, proxy);
    }

    /**
     * 判断Activity是否有在宿主中声明
     * @param activity
     * @return
     */
    private static boolean isExistActivity(String activity) {
        try {
            PackageManager packageManager = HostApplication.getContext().getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(HostApplication.getContext().getPackageName(), PackageManager.GET_ACTIVITIES);
            ActivityInfo[] activities = packageInfo.activities;
            for(int i = 0; i < activities.length; i++) {
                if (activity.equalsIgnoreCase(activities[i].name)) {
                    return true;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return  false;
    }

    /**
     * 第二处Hook地方，Hook ActivityThread 中的 Handle mCallback 对象，使接收 LAUNCH_ACTIVITY 消息后将替身 Activity 换回目标 Activity
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     */
    private static void hookActivityThread() throws ClassNotFoundException, IllegalAccessException, NoSuchFieldException  {

        // 获取到当前的 ActivityThread 对象
        Class activityThreadClass = Class.forName("android.app.ActivityThread");
        Field sCurrentActivityThreadField = activityThreadClass.getDeclaredField("sCurrentActivityThread");
        sCurrentActivityThreadField.setAccessible(true);
        Object currentActivityThreadObj = sCurrentActivityThreadField.get(null);

        // 获取 ActivityThread 对象中的 mH 变量
        Field mHField = activityThreadClass.getDeclaredField("mH");
        mHField.setAccessible(true);
        final Handler mHObj = (Handler) mHField.get(currentActivityThreadObj);

        // Hook Handler 的 mCallback 字段
        Class handlerClass = Handler.class;
        Field mCallbackField = handlerClass.getDeclaredField("mCallback");
        mCallbackField.setAccessible(true);
        mCallbackField.set(mHObj, new Handler.Callback() {

            @Override
            public boolean handleMessage(Message msg) {

                int LAUNCH_ACTIVITY = 0;
                try {
                    Class hClass = Class.forName("android.app.ActivityThread$H");
                    Field launchActivityField = hClass.getDeclaredField("LAUNCH_ACTIVITY");
                    launchActivityField.setAccessible(true);
                    LAUNCH_ACTIVITY = (int) launchActivityField.get(null);

                    if (msg.what == LAUNCH_ACTIVITY) {
                        // 把替身 Activity 的 Intent 恢复成目标 Activity 的 Intent
                        Class c = msg.obj.getClass();
                        Field intentField = c.getDeclaredField("intent");
                        intentField.setAccessible(true);
                        Intent intent = (Intent) intentField.get(msg.obj);
                        Intent targetIntent = intent.getParcelableExtra(EXTRA_TARGET_INTENT);
                        if (targetIntent != null) {
                            intent.setComponent(targetIntent.getComponent());
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // 走完原来流程
                mHObj.handleMessage(msg);
                return true;
            }
        });
    }
}
