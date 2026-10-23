package com.example.it381_project;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * FileHelper handles file reading and writing operations
 * using Android Internal Storage based on Lab 3 slide patterns.
 */
public class FileHelper {
    private static final String TAG = "FileHelper";
    public static final String ATTENDANCE_FILE = "attendance_summary.txt";
    public static final String EVENTS_SUMMARY_FILE = "events_summary.txt";

    /**
     * Write text content to internal storage file using FileOutputStream
     * following the Lab 3 slide implementation.
     */
    public static boolean writeToFile(Context context, String filename, String data) {
        try {
            File file = new File(context.getFilesDir(), filename);
            FileOutputStream outputStream = new FileOutputStream(file);
            outputStream.write(data.getBytes());
            outputStream.close();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error writing to file " + filename, e);
            return false;
        }
    }

    /**
     * Read text content from internal storage file using FileInputStream and byte buffer
     * following the Lab 3 slide implementation.
     */
    public static String readFromFile(Context context, String filename) {
        try {
            File file = new File(context.getFilesDir(), filename);
            if (!file.exists()) {
                return null;
            }
            byte[] buffer = new byte[(int) file.length()];
            FileInputStream inputStream = new FileInputStream(file);
            inputStream.read(buffer);
            inputStream.close();
            return new String(buffer);
        } catch (IOException e) {
            Log.e(TAG, "Error reading file " + filename, e);
            return null;
        }
    }

    /**
     * Checks if a file exists in internal storage
     */
    public static boolean fileExists(Context context, String filename) {
        File file = new File(context.getFilesDir(), filename);
        return file.exists();
    }
}
