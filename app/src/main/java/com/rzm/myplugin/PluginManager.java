package com.rzm.myplugin;


import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import dalvik.system.DexClassLoader;

/**
 * android 28
 * <p>
 * package dalvik.system;
 * public class BaseDexClassLoader extends ClassLoader {
 * private final DexPathList pathList;
 * }
 * <p>
 * package dalvik.system;
 * final class DexPathList {
 * private Element[] dexElements;
 * }
 * <p>
 * package dalvik.system;
 * public class PathClassLoader extends BaseDexClassLoader {
 * public PathClassLoader(String dexPath, String librarySearchPath, ClassLoader parent) {
 * super(dexPath, null, librarySearchPath, parent);
 * }
 * }
 * <p>
 * package dalvik.system;
 * public class DexClassLoader extends BaseDexClassLoader {
 * public DexClassLoader(String dexPath, String optimizedDirectory,
 * String librarySearchPath, ClassLoader parent) {
 * super(dexPath, null, librarySearchPath, parent);
 * }
 * }
 * <p>
 * 1.获取宿主dexElements
 * 2.获取插件dexElements
 * 3.合并两个dexElements
 * 4.将新的dexElements 赋值到 宿主dexElements
 */
public class PluginManager {

    private static final String FINAL_INTENT = "final_intent";
    private static String dexPath = "/sdcard/plugin.apk";

    /**
     * Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
     */
    public static void hookAms() {
        try {
            //获取singleTon对象
            Field singletonField;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                Class<?> clazz = Class.forName("android.app.ActivityManagerNative");
                singletonField = clazz.getDeclaredField("gDefault");
            } else {
                Class<?> activityManagerClass = Class.forName("android.app.ActivityManager");
                singletonField = activityManagerClass.getDeclaredField("IActivityManagerSingleton");
            }
            singletonField.setAccessible(true);
            Object singletonObj = singletonField.get(null);

            //获取系统的ActivityManager对象
            Class<?> singleTonClass = Class.forName("android.util.Singleton");
            Field activityManagerField = singleTonClass.getDeclaredField("mInstance");
            activityManagerField.setAccessible(true);
            Object activityManagerObj = activityManagerField.get(singletonObj);

            Class<?> iActivityManagerClass = Class.forName("android.app.IActivityManager");
            Object activityManagerProxyObj = Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), new Class[]{iActivityManagerClass}, (proxy, method, args) -> {
                if ("startActivity".equals(method.getName())) {
                    System.out.println("PluginManager invoke method = " + method);
                    int index = -1;
                    for (int i = 0; i < args.length; i++) {
                        if (args[i] instanceof Intent) {
                            index = i;
                        }
                    }
                    if (index != -1) {
                        Intent finalIntent = (Intent) args[index];
                        Intent proxyIntent = new Intent();
                        proxyIntent.setClassName("com.rzm.myplugin",
                                "com.rzm.myplugin.ProxyActivity");
                        proxyIntent.putExtra(FINAL_INTENT, finalIntent);
                        args[index] = proxyIntent;
                    }
                }
                return method.invoke(activityManagerObj, args);
            });
            //代理对象设置给系统
            activityManagerField.set(singletonObj, activityManagerProxyObj);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void hookHandler() {
        try {
            Handler.Callback callback = msg -> {
                switch (msg.what) {
                    case 100:
                        try {
                            Field intentField = msg.obj.getClass().getDeclaredField("intent");
                            intentField.setAccessible(true);
                            // 启动代理Intent
                            Intent proxyIntent = (Intent) intentField.get(msg.obj);
                            // 启动插件的 Intent
                            Intent intent = proxyIntent.getParcelableExtra(FINAL_INTENT);
                            if (intent != null) {
                                intentField.set(msg.obj, intent);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case 159:
                        try {
                            // 获取 mActivityCallbacks 对象
                            Field mActivityCallbacksField = msg.obj.getClass()
                                    .getDeclaredField("mActivityCallbacks");
                            mActivityCallbacksField.setAccessible(true);
                            List mActivityCallbacks = (List) mActivityCallbacksField.get(msg.obj);

                            for (int i = 0; i < mActivityCallbacks.size(); i++) {
                                if (mActivityCallbacks.get(i).getClass().getName()
                                        .equals("android.app.servertransaction.LaunchActivityItem")) {
                                    Object launchActivityItem = mActivityCallbacks.get(i);

                                    // 获取启动代理的 Intent
                                    Field mIntentField = launchActivityItem.getClass()
                                            .getDeclaredField("mIntent");
                                    mIntentField.setAccessible(true);
                                    Intent proxyIntent = (Intent) mIntentField.get(launchActivityItem);

                                    // 目标 intent 替换 proxyIntent
                                    Intent intent = proxyIntent.getParcelableExtra(FINAL_INTENT);
                                    if (intent != null) {
                                        mIntentField.set(launchActivityItem, intent);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                }

                return false;
            };
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Field sCurrentActivityThreadField = activityThreadClass.getDeclaredField("sCurrentActivityThread");
            sCurrentActivityThreadField.setAccessible(true);
            Object sCurrentActivityThreadObj = sCurrentActivityThreadField.get(null);

            Field mHField = activityThreadClass.getDeclaredField("mH");
            mHField.setAccessible(true);
            Object mHandlerObj = mHField.get(sCurrentActivityThreadObj);


            Class<?> handlerClass = Class.forName("android.os.Handler");
            Field mCallbackField = handlerClass.getDeclaredField("mCallback");
            mCallbackField.setAccessible(true);
            mCallbackField.set(mHandlerObj, callback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadPlugin(Context context) throws Exception {
        //3.获取BaseDexClassLoader对象，宿主的classloader
        ClassLoader classLoader = context.getClassLoader();

        //2.获取DexPathList对象，需要用到BaseDexClassLoader对象
        Class<?> baseDexClassLoaderClass = Class.forName("dalvik.system.BaseDexClassLoader");
        Field pathListField = baseDexClassLoaderClass.getDeclaredField("pathList");
        pathListField.setAccessible(true);
        Object pathListObj = pathListField.get(classLoader);

        //1.获取dexElements对象(宿主的)，需要用到DexPathList对象
        Class<?> dexPathListClass = Class.forName("dalvik.system.DexPathList");
        Field dexElementsField = dexPathListClass.getDeclaredField("dexElements");
        dexElementsField.setAccessible(true);
        Object[] dexElementsObj = (Object[]) dexElementsField.get(pathListObj);

        //4.获取插件的dexElements对象，需要用到DexPathList对象，由上边的123部反推，DexPathList对象需要插件
        //的BaseDexClassLoader对象，那么我们需要构建一个ClassLoader去加载插件
        DexClassLoader dexClassLoader = new DexClassLoader(dexPath,
                context.getCacheDir().getAbsolutePath(), null, classLoader);

        //5.插件的dexElements对象
        Object pluginPathListObj = pathListField.get(dexClassLoader);
        Object[] pluginDexElementObj = (Object[]) dexElementsField.get(pluginPathListObj);


        // 创建一个新数组
        Object[] newDexElements = (Object[]) Array.newInstance(dexElementsObj.getClass().getComponentType(),
                dexElementsObj.length + pluginDexElementObj.length);

        System.arraycopy(dexElementsObj, 0, newDexElements,
                0, dexElementsObj.length);
        System.arraycopy(pluginDexElementObj, 0, newDexElements,
                dexElementsObj.length, pluginDexElementObj.length);

        // 赋值
        dexElementsField.set(pathListObj, newDexElements);
    }
}
