package com.zyx.plugindemo;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;

import com.zyx.plugindemo.Common.PluginManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;

public class ProxyActivity extends Activity {

    public final static String TRAGET_ACTIVITY_CLASS_NAME = "tragetActivityClassName";

    private Object mRemoteActivity;
    private String mTragetActivityClassName;
    private HashMap<String, Method> mActivityLifecircleMethods = new HashMap<String, Method>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTragetActivityClassName = getIntent().getStringExtra(TRAGET_ACTIVITY_CLASS_NAME);

        launchTargetActivity();
        invokeOnCreate();
    }

    @Override
    public Resources getResources() {
        if (PluginManager.getResources() != null) {
            return PluginManager.getResources();
        } else {
            return super.getResources();
        }
    }

    /**
     * 反射目标 Activity
     */
    void launchTargetActivity() {
        try {
            // 获取插件的 Activity 对象
            Class<?> localClass = Class.forName(mTragetActivityClassName);
            Constructor<?> localConstructor = localClass.getConstructor(new Class[] {});
            mRemoteActivity = localConstructor.newInstance(new Object[] {});

            // 执行插件 Activity 的 setProxy 方法，传递this过去，使建立双向引用
            Method setProxy = localClass.getMethod("setProxy", new Class[] { Activity.class });
            setProxy.setAccessible(true);
            setProxy.invoke(mRemoteActivity, new Object[] { this });

            // 反射插件 Activity 的生命周期函数
            launchTargetActivityLifecircleMethods(localClass);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 反射插件 Activity 的生命周期函数
     * @param localClass
     */
    protected void launchTargetActivityLifecircleMethods(Class<?> localClass) {
        Method onCreate = null;
        try {
            onCreate = localClass.getDeclaredMethod("onCreate", new Class[] { Bundle.class });
            onCreate.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        mActivityLifecircleMethods.put("onCreate", onCreate);

        String[] methodNames = new String[]{"onRestart", "onStart", "onResume", "onPause", "onStop", "onDestory"};
        for (String methodName : methodNames) {
            Method method = null;
            try {
                method = localClass.getDeclaredMethod(methodName, new Class[]{});
                method.setAccessible(true);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
            mActivityLifecircleMethods.put(methodName, method);
        }

        Method onActivityResult = null;
        try {
            onActivityResult = localClass.getDeclaredMethod("onActivityResult",
                    new Class[] { int.class, int.class, Intent.class });
            onActivityResult.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        mActivityLifecircleMethods.put("onActivityResult", onActivityResult);
    }

    /**
     * 执行插件 Activity 的 onCreate 方法
     */
    private void invokeOnCreate() {
        Method onCreate = mActivityLifecircleMethods.get("onCreate");
        if (onCreate != null) {
            try {
                Bundle bundle = new Bundle();
                onCreate.invoke(mRemoteActivity, new Object[]{bundle});
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        invokeOnActivityResult(requestCode, resultCode, data);
    }

    private void invokeOnActivityResult(int requestCode, int resultCode, Intent data) {
        Method onActivityResult = mActivityLifecircleMethods.get("onActivityResult");
        if (onActivityResult != null) {
            try {
                onActivityResult.invoke(mRemoteActivity, new Object[] { requestCode, resultCode, data });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        invokeOnStart();
    }

    private void invokeOnStart() {
        Method onStart = mActivityLifecircleMethods.get("onStart");
        if (onStart != null) {
            try {
                onStart.invoke(mRemoteActivity, new Object[] {});
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        invokeOnRestart();
    }

    private void invokeOnRestart() {
        Method onRestart = mActivityLifecircleMethods.get("onRestart");
        if (onRestart != null) {
            try {
                onRestart.invoke(mRemoteActivity, new Object[] { });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        invokeOnResume();
    }

    private void invokeOnResume() {
        Method onResume = mActivityLifecircleMethods.get("onResume");
        if (onResume != null) {
            try {
                onResume.invoke(mRemoteActivity, new Object[] { });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        invokeOnPause();
    }

    private void invokeOnPause() {
        Method onPause = mActivityLifecircleMethods.get("onPause");
        if (onPause != null) {
            try {
                onPause.invoke(mRemoteActivity, new Object[] { });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        invokeOnStop();
    }

    private void invokeOnStop() {
        Method onStop = mActivityLifecircleMethods.get("onStop");
        if (onStop != null) {
            try {
                onStop.invoke(mRemoteActivity, new Object[] { });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        invokeOnDestroy();
    }

    private void invokeOnDestroy() {
        Method onDestroy = mActivityLifecircleMethods.get("onDestroy");
        if (onDestroy != null) {
            try {
                onDestroy.invoke(mRemoteActivity, new Object[] { });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
