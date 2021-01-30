package com.rzm.myplugin;


import android.content.Context;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;

import dalvik.system.DexClassLoader;

/**
 * android 28
 *
 *  package dalvik.system;
 *  public class BaseDexClassLoader extends ClassLoader {
 *      private final DexPathList pathList;
 *  }
 *
 *  package dalvik.system;
 *  final class DexPathList {
 *      private Element[] dexElements;
 *  }
 *
 *  package dalvik.system;
 *  public class PathClassLoader extends BaseDexClassLoader {
 *     public PathClassLoader(String dexPath, String librarySearchPath, ClassLoader parent) {
 *         super(dexPath, null, librarySearchPath, parent);
 *     }
 *  }
 *
 *  package dalvik.system;
 *  public class DexClassLoader extends BaseDexClassLoader {
 *     public DexClassLoader(String dexPath, String optimizedDirectory,
 *             String librarySearchPath, ClassLoader parent) {
 *         super(dexPath, null, librarySearchPath, parent);
 *     }
 *  }
 *
 *  1.获取宿主dexElements
 *  2.获取插件dexElements
 *  3.合并两个dexElements
 *  4.将新的dexElements 赋值到 宿主dexElements
 */
public class PluginManager {

    private static String dexPath = "/sdcard/plugin.apk";


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
                context.getCacheDir().getAbsolutePath(),null,classLoader);

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
