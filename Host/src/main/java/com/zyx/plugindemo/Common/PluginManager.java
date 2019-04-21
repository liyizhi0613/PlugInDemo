package com.zyx.plugindemo.Common;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import dalvik.system.BaseDexClassLoader;
import dalvik.system.DexFile;

public class PluginManager {

    public final static String PLUGIN_NAME = "PlugIn-debug.apk";
    private static Resources sResources;

    /**
     * 插件加载入口
     * @param applicationBaseContext
     */
    public static void load(Context applicationBaseContext) {

        try {
            mergePluginDex(applicationBaseContext, PLUGIN_NAME);
            Resources newResources = mergePluginResources(applicationBaseContext, PLUGIN_NAME);
            initPlugIn(applicationBaseContext, newResources);

            sResources = newResources;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Resources getResources() {
        return sResources;
    }

    /**
     * 合并插件中的代码
     * @param context
     * @param apkName
     * @throws IllegalAccessException
     * @throws NoSuchMethodException
     * @throws IOException
     * @throws InvocationTargetException
     * @throws InstantiationException
     * @throws NoSuchFieldException
     */
    private static void mergePluginDex(Context context, String apkName)
            throws IllegalAccessException, NoSuchMethodException, IOException, InvocationTargetException, InstantiationException, NoSuchFieldException {

        ClassLoader pathClassLoaderClass = context.getClassLoader();

        // 获取 PathClassLoader(BaseDexClassLoader) 的 DexPathList 对象变量 pathList
        Class baseDexClassLoaderClass = BaseDexClassLoader.class;
        Field pathListField = baseDexClassLoaderClass.getDeclaredField("pathList");
        pathListField.setAccessible(true);
        Object pathListObj = pathListField.get(pathClassLoaderClass);

        // 获取 DexPathList 的 Element[] 对象变量 dexElements
        Class dexPathListClass = pathListObj.getClass();
        Field dexElementsField = dexPathListClass.getDeclaredField("dexElements");
        dexElementsField.setAccessible(true);
        Object[] dexElementListObj = (Object[]) dexElementsField.get(pathListObj);

        // 获得 Element 类型
        Class<?> elementClass = dexElementListObj.getClass().getComponentType();

        // 创建一个新的Element[], 将用于替换原始的数组
        Object[] newElementListObj = (Object[]) Array.newInstance(elementClass, dexElementListObj.length + 1);

        Object pluginElementObj = null;
        File apkFile = context.getFileStreamPath(apkName);
        File optDexFile = context.getFileStreamPath(apkName.replace(".apk", ".dex"));
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // 构造插件的 Element，构造函数参数：(File file, boolean isDirectory, File zip, DexFile dexFile)
            Class[] paramClass = {File.class, boolean.class, File.class, DexFile.class};
            Object[] paramValue = {apkFile, false, apkFile, DexFile.loadDex(apkFile.getCanonicalPath(), optDexFile.getAbsolutePath(), 0)};
            Constructor elementCtor = elementClass.getDeclaredConstructor(paramClass);
            elementCtor.setAccessible(true);
            pluginElementObj = elementCtor.newInstance(paramValue);
        } else {
            // 构造插件的 Element，构造函数参数：(DexFile dexFile, File file)
            Class[] paramClass = {DexFile.class, File.class};
            Object[] paramValue = {DexFile.loadDex(apkFile.getCanonicalPath(), optDexFile.getAbsolutePath(), 0), apkFile};
            Constructor elementCtor = elementClass.getDeclaredConstructor(paramClass);
            elementCtor.setAccessible(true);
            pluginElementObj = elementCtor.newInstance(paramValue);
        }
        Object[] pluginElementListObj = new Object[]{pluginElementObj};

        // 把原来 PathClassLoader 中的 elements 复制进去新的Element[]中
        System.arraycopy(dexElementListObj, 0, newElementListObj, 0, dexElementListObj.length);
        // 把插件的 element 复制进去新的 Element[] 中
        System.arraycopy(pluginElementListObj, 0, newElementListObj, dexElementListObj.length, pluginElementListObj.length);

        // 替换原来 PathClassLoader 中的 dexElements 值
        Field field = pathListObj.getClass().getDeclaredField("dexElements");
        field.setAccessible(true);
        field.set(pathListObj, newElementListObj);
    }

    /**
     * 合并插件中的资源
     * @param applicationBaseContext
     * @param apkName
     * @return 合并后新的资源对象
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws NoSuchFieldException
     */
    private static Resources mergePluginResources(Context applicationBaseContext, String apkName)
            throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException, NoSuchFieldException {
        // 创建一个新的 AssetManager 对象
        AssetManager newAssetManagerObj = AssetManager.class.newInstance();
        Method addAssetPath = AssetManager.class.getMethod("addAssetPath", String.class);
        // 塞入原来宿主的资源
        addAssetPath.invoke(newAssetManagerObj, applicationBaseContext.getPackageResourcePath());
        // 塞入插件的资源
        File optDexFile = applicationBaseContext.getFileStreamPath(apkName);
        addAssetPath.invoke(newAssetManagerObj, optDexFile.getAbsolutePath());

        // ----------------------------------------------

        // 创建一个新的 Resources 对象
        Resources newResourcesObj = new Resources(newAssetManagerObj,
                applicationBaseContext.getResources().getDisplayMetrics(),
                applicationBaseContext.getResources().getConfiguration());

        // ----------------------------------------------

        // 获取 ContextImpl 中的 Resources 类型的 mResources 变量，并替换它的值为新的 Resources 对象
        Field resourcesField = applicationBaseContext.getClass().getDeclaredField("mResources");
        resourcesField.setAccessible(true);
        resourcesField.set(applicationBaseContext, newResourcesObj);

        // ----------------------------------------------

        // 获取 ContextImpl 中的 LoadedApk 类型的 mPackageInfo 变量
        Field packageInfoField = applicationBaseContext.getClass().getDeclaredField("mPackageInfo");
        packageInfoField.setAccessible(true);
        Object packageInfoObj = packageInfoField.get(applicationBaseContext);

        // 获取 mPackageInfo 变量对象中类的 Resources 类型的 mResources 变量，，并替换它的值为新的 Resources 对象
        // 注意：这是最主要的需要替换的，如果不需要支持插件运行时更新，只留这一个就可以了
        Field resourcesField2 = packageInfoObj.getClass().getDeclaredField("mResources");
        resourcesField2.setAccessible(true);
        resourcesField2.set(packageInfoObj, newResourcesObj);

        // ----------------------------------------------

        // 获取 ContextImpl 中的 Resources.Theme 类型的 mTheme 变量，并至空它
        // 注意：清理mTheme对象，否则通过inflate方式加载资源会报错, 如果是activity动态加载插件，则需要把activity的mTheme对象也设置为null
        Field themeField = applicationBaseContext.getClass().getDeclaredField("mTheme");
        themeField.setAccessible(true);
        themeField.set(applicationBaseContext, null);

        return newResourcesObj;
    }

    /**
     * 初始化插件
     * @param applicationBaseContext
     * @param resources
     * @throws ClassNotFoundException
     */
    private static void initPlugIn(Context applicationBaseContext, Resources resources)
            throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Class plugInApplicationClass = Class.forName("com.zyx.plugin.PlugInApplication");
        Class[] paramClass = {Context.class, Resources.class};
        Method initMethod = plugInApplicationClass.getMethod("init", paramClass);
        initMethod.setAccessible(true);
        Object[] paramValue = {applicationBaseContext, resources};
        initMethod.invoke(null, paramValue);
    }
}
