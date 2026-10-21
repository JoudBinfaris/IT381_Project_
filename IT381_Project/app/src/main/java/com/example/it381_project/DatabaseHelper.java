package com.example.it381_project;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * DatabaseHelper for SQLite storage.
 * Manages database creation, upgrades, and CRUD operations
 * for registered events and attendance records.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String TABLE_NAME = "registered_events";
    public static final String COL_ID = "ID";
    public static final String col1 = "name";
    public static final String COL_DATE = "date";
    public static final String COL_LOCATION = "location";
    public static final String COL_DESCRIPTION = "description";

    // Attendance Table
    public static final String TABLE_ATTENDANCE = "attendance_table";
    public static final String COL_ATT_ID = "ID";
    public static final String COL_PARTICIPANT_NAME = "participant_name";
    public static final String COL_EVENT_NAME = "event_name";
    public static final String COL_DATE_TIME = "date_time";

    public DatabaseHelper(Context context) {
        super(context, TABLE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Table for registered events
        String createTable = "CREATE TABLE " + TABLE_NAME
                + " (" + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + col1 + " TEXT, "
                + COL_DATE + " TEXT, "
                + COL_LOCATION + " TEXT, "
                + COL_DESCRIPTION + " TEXT)";
        db.execSQL(createTable);

        // Table for Bluetooth attendance records
        String createAttendance = "CREATE TABLE " + TABLE_ATTENDANCE
                + " (" + COL_ATT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_PARTICIPANT_NAME + " TEXT, "
                + COL_EVENT_NAME + " TEXT, "
                + COL_DATE_TIME + " TEXT)";
        db.execSQL(createAttendance);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ATTENDANCE);
        onCreate(db);
    }

    /**
     * Adds an item into col1 of TABLE_NAME
     */
    public boolean addData(String item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(col1, item);
        contentValues.put(COL_DATE, "N/A");
        contentValues.put(COL_LOCATION, "N/A");
        contentValues.put(COL_DESCRIPTION, "General Item Added");
        long result = db.insert(TABLE_NAME, null, contentValues);
        return result != -1;
    }

    /**
     * Returns Cursor for TABLE_NAME
     */
    public Cursor getData() {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + TABLE_NAME;
        Cursor data = db.rawQuery(query, null);
        return data;
    }

    /**
     * Extended registration method to insert full event details
     */
    public boolean addRegisteredEvent(String name, String date, String location, String description) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(col1, name);
        contentValues.put(COL_DATE, date);
        contentValues.put(COL_LOCATION, location);
        contentValues.put(COL_DESCRIPTION, description);
        long result = db.insert(TABLE_NAME, null, contentValues);
        return result != -1;
    }

    public Cursor getRegisteredEvents() {
        return getData();
    }

    public boolean isEventRegistered(String name) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE " + col1 + " = ?", new String[]{name});
        boolean exists = (cursor != null && cursor.getCount() > 0);
        if (cursor != null) cursor.close();
        return exists;
    }

    public boolean deleteEvent(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int affected = db.delete(TABLE_NAME, COL_ID + " = ?", new String[]{String.valueOf(id)});
        return affected > 0;
    }

    /**
     * Insert attendance record
     */
    public boolean addAttendance(String participantName, String eventName, String dateTime) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_PARTICIPANT_NAME, participantName);
        cv.put(COL_EVENT_NAME, eventName);
        cv.put(COL_DATE_TIME, dateTime);
        long res = db.insert(TABLE_ATTENDANCE, null, cv);
        return res != -1;
    }

    /**
     * Retrieve attendance records
     */
    public Cursor getAttendanceData() {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + TABLE_ATTENDANCE + " ORDER BY " + COL_ATT_ID + " DESC";
        return db.rawQuery(query, null);
    }

    public boolean deleteAttendance(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int affected = db.delete(TABLE_ATTENDANCE, COL_ATT_ID + " = ?", new String[]{String.valueOf(id)});
        return affected > 0;
    }

    public int getAttendanceCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_ATTENDANCE, null);
        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            cursor.close();
        }
        return count;
    }

    public int getRegisteredEventsCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME, null);
        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            cursor.close();
        }
        return count;
    }
}
