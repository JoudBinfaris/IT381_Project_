package com.example.it381_project;

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * FileStorageActivity: Demonstrates Android file reading and writing operations.
 * Fulfills IT381 Project Requirement 6:
 * - Export attendance summary to file
 * - Save registered event summary to file
 * - Read and display previously saved reports
 */
public class FileStorageActivity extends AppCompatActivity {

    private Button btnBack;
    private Button btnExportAttendance, btnExportEvents;
    private Button btnReadAttendance, btnReadEvents;
    private TextView tvWriteStatus, tvReportTitle, tvReportContent;

    private DatabaseHelper mDatabaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_storage);

        mDatabaseHelper = new DatabaseHelper(this);

        btnBack = findViewById(R.id.btnBack);
        btnExportAttendance = findViewById(R.id.btnExportAttendance);
        btnExportEvents = findViewById(R.id.btnExportEvents);
        btnReadAttendance = findViewById(R.id.btnReadAttendance);
        btnReadEvents = findViewById(R.id.btnReadEvents);
        tvWriteStatus = findViewById(R.id.tvWriteStatus);
        tvReportTitle = findViewById(R.id.tvReportTitle);
        tvReportContent = findViewById(R.id.tvReportContent);

        btnBack.setOnClickListener(v -> finish());

        // 1. Export Attendance Summary to File
        btnExportAttendance.setOnClickListener(v -> exportAttendanceToFile());

        // 2. Export Registered Events to File
        btnExportEvents.setOnClickListener(v -> exportEventsToFile());

        // 3. Read Attendance Summary from File
        btnReadAttendance.setOnClickListener(v -> readReportFromFile(FileHelper.ATTENDANCE_FILE, "Attendance Summary Report"));

        // 4. Read Events Summary from File
        btnReadEvents.setOnClickListener(v -> readReportFromFile(FileHelper.EVENTS_SUMMARY_FILE, "Registered Events Summary Report"));
    }

    private void exportAttendanceToFile() {
        Cursor cursor = mDatabaseHelper.getAttendanceData();
        if (cursor == null || cursor.getCount() == 0) {
            Toast.makeText(this, "No attendance records found in SQLite to export", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder sb = new StringBuilder();
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        sb.append("====================================================\n");
        sb.append(" SMART EVENT COMPANION - ATTENDANCE SUMMARY REPORT\n");
        sb.append("====================================================\n");
        sb.append("Generated On: ").append(timeStamp).append("\n");
        sb.append("Total Attendees Recorded: ").append(cursor.getCount()).append("\n");
        sb.append("Storage Engine: SQLite + Internal File Storage\n");
        sb.append("----------------------------------------------------\n\n");

        int index = 1;
        while (cursor.moveToNext()) {
            String pName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PARTICIPANT_NAME));
            String eName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EVENT_NAME));
            String dTime = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DATE_TIME));

            sb.append(String.format(Locale.getDefault(), "[%02d] Attendee: %s\n", index++, pName));
            sb.append("     Event:    ").append(eName).append("\n");
            sb.append("     Time:     ").append(dTime).append("\n\n");
        }
        cursor.close();

        boolean success = FileHelper.writeToFile(this, FileHelper.ATTENDANCE_FILE, sb.toString());
        if (success) {
            tvWriteStatus.setVisibility(View.VISIBLE);
            tvWriteStatus.setText("✓ Successfully written to " + FileHelper.ATTENDANCE_FILE);
            Toast.makeText(this, "Attendance written to file!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to write attendance report", Toast.LENGTH_SHORT).show();
        }
    }

    private void exportEventsToFile() {
        Cursor cursor = mDatabaseHelper.getData();
        if (cursor == null || cursor.getCount() == 0) {
            Toast.makeText(this, "No registered events found in SQLite to write", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder sb = new StringBuilder();
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        sb.append("====================================================\n");
        sb.append(" SMART EVENT COMPANION - REGISTERED EVENTS REPORT\n");
        sb.append("====================================================\n");
        sb.append("Generated On: ").append(timeStamp).append("\n");
        sb.append("Total Events Registered: ").append(cursor.getCount()).append("\n");
        sb.append("Storage Engine: SQLite + Internal File Storage\n");
        sb.append("----------------------------------------------------\n\n");

        int index = 1;
        while (cursor.moveToNext()) {
            String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.col1));
            String date = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DATE));
            String location = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_LOCATION));
            String desc = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DESCRIPTION));

            sb.append(String.format(Locale.getDefault(), "[%02d] Event: %s\n", index++, name));
            sb.append("     Date:     ").append(date).append("\n");
            sb.append("     Location: ").append(location).append("\n");
            sb.append("     Details:  ").append(desc).append("\n\n");
        }
        cursor.close();

        boolean success = FileHelper.writeToFile(this, FileHelper.EVENTS_SUMMARY_FILE, sb.toString());
        if (success) {
            tvWriteStatus.setVisibility(View.VISIBLE);
            tvWriteStatus.setText("✓ Successfully written to " + FileHelper.EVENTS_SUMMARY_FILE);
            Toast.makeText(this, "Events written to file!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to write events report", Toast.LENGTH_SHORT).show();
        }
    }

    private void readReportFromFile(String filename, String title) {
        if (!FileHelper.fileExists(this, filename)) {
            Toast.makeText(this, "File not found: " + filename + ". Write file first!", Toast.LENGTH_LONG).show();
            tvReportTitle.setText(title + " (File not found)");
            tvReportContent.setText("No file exists yet. Please tap the Write button above to create this file.");
            return;
        }

        String content = FileHelper.readFromFile(this, filename);
        if (content != null) {
            tvReportTitle.setText("Reading File: " + filename);
            tvReportContent.setText(content);
            Toast.makeText(this, "File read successfully (" + filename + ")", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to read file: " + filename, Toast.LENGTH_SHORT).show();
        }
    }
}
