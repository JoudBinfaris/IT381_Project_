package com.example.it381_project;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * FileHelper handles file reading and writing operations
 * using Android Internal Storage.
 */
public class FileHelper {
    private static final String TAG = "FileHelper";
    public static final String ATTENDANCE_FILE = "attendance_summary.txt";
    public static final String EVENTS_SUMMARY_FILE = "events_summary.txt";

    /**
     * Write text content to internal storage file
     */
    public static boolean writeToFile(Context context, String filename, String data) {
        try (FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE)) {
            fos.write(data.getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error writing to file " + filename, e);
            return false;
        }
    }

    /**
     * Read text content from internal storage file
     */
    public static String readFromFile(Context context, String filename) {
        StringBuilder sb = new StringBuilder();
        try (FileInputStream fis = context.openFileInput(filename);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr)) {

            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error reading file " + filename, e);
            return null;
        }
    }

    /**
     * Checks if a file exists in internal storage
     */
    public static boolean fileExists(Context context, String filename) {
        return context.getFileStreamPath(filename).exists();
    }
}
