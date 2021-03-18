package com.tencent.qgame.playerproj.animtool;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Description:文件重命名 Created by 杜小菜 on 2021/2/23 - 11:47 . E-mail: duqian2010@gmail.com
 */
public class FileUtils {

    public static void renameImages(String rootDir) {
        File file = new File(rootDir);
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                if (f.isDirectory()) {
                    String imageDir = f.getAbsolutePath();
                    System.out.println("imageDir = " + imageDir);
                    renameFileInRootDir(imageDir);
                }
            }
        }
    }

    public static void renameImages(String... imagePath) {
        //当作数组用foreach遍历
        for (String path : imagePath) {
            System.out.println("imagePath = " + path);
            renameFileInRootDir(path);
        }
    }

    public static void renameRes(String maskPath, String pngPath) {
        renameFileInRootDir(maskPath);
        renameFileInRootDir(pngPath);
    }

    public static void renameFileInRootDir(String rootFilePath) {
        try {
            File rootFile = new File(rootFilePath);
            if (rootFile.isDirectory()) {
                String[] list = rootFile.list();
                if (list == null || list.length == 0) {
                    return;
                }
                for (int i = 0; i < list.length; i++) {
                    //这个路径是没有根目录的
                    String currentPath = list[i];
                    String currentFileName = currentPath;
                    int length = currentFileName.length();
                    String extension = currentFileName.substring(currentFileName.lastIndexOf("."), length);
                    String newFileName = formatName(i) + extension;
                    String newPath = rootFilePath + File.separator + File.separator + newFileName;
                    File currentFile = new File(rootFilePath + File.separator + currentPath);
                    boolean renameTo = currentFile.renameTo(new File(newPath));
                    System.out.println("newPath=" + newPath + ",renameTo=" + renameTo);

                    //重命名失败，那就拷贝吧
                    if (!renameTo) {
                        boolean copyFile = copyFile(currentFile, newPath);
                        System.out.println("newPath=" + newPath + ",copyFile=" + copyFile);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean copyFile(File currentFile, String newPath) {
        if (currentFile == null || "".equals(newPath) || newPath == null) {
            return false;
        }
        File targetFile = new File(newPath);
        FileInputStream fileInputStream = null;
        FileOutputStream fileOutputStream = null;
        try {
            fileInputStream = new FileInputStream(currentFile);
            fileOutputStream = new FileOutputStream(targetFile);
            byte[] buffer = new byte[1024];
            int length = -1;
            while ((length = fileInputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, length);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fileInputStream.close();
                fileOutputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private static String formatName(int i) {
        return String.format(Locale.getDefault(), "%03d", i);
    }


}
