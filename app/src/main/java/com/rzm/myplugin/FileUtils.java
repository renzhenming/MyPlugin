package com.enjoy.leo_plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileUtils {

    private static boolean copyFile(File srcFile, File destFile) {
        FileInputStream is = null;
        FileOutputStream os = null;
        try {
            is = new FileInputStream(srcFile);
            byte buf[] = new byte[8 * 1024];
            int count = 0;
            os = new FileOutputStream(destFile);
            while ((count = is.read(buf)) != -1) {
                os.write(buf, 0, count);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

        }
        return false;
    }
    private static boolean copyFile(InputStream is, File destFile) {
        FileOutputStream os = null;
        try {
            byte buf[] = new byte[8 * 1024];
            int count = 0;
            os = new FileOutputStream(destFile);
            while ((count = is.read(buf)) != -1) {
                os.write(buf, 0, count);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

        }
        return false;
    }
}
