package com.rzm.myplugin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
        }
    }

    private void onPermissionGet() {

    }

    public void jump(View view) {
        PluginManager.hookAms();
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.rzm.myplugin.plugin","com.rzm.plugin.TestActivity"));
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }
            new Thread(() -> {
                try {
                    File file = new File(Environment.getExternalStorageDirectory() + "/plugin.apk");
                    boolean success = FileUtils.copyFile(getAssets().open("plugin.apk"), file);
                    System.out.println("MainActivity copy assets success = " + success);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    PluginManager.loadPlugin(MainActivity.this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    Class<?> aClass = Class.forName("com.rzm.plugin.PluginClass1");
                    Method print = aClass.getDeclaredMethod("print");
                    Object o = aClass.newInstance();
                    print.invoke(o);
                    System.out.println("MainActivity o = " + o);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
            onPermissionGet();
        }
    }

}