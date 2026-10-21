package com.example.it381_project;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RegisteredEventsActivity: Retrieves and displays all registered events stored in SQLite.
 */
public class RegisteredEventsActivity extends AppCompatActivity {

    private Button btnBack;
    private TextView tvRegisteredSummary, tvEmptyRegistered;
    private ListView lvRegisteredEvents;
    private Button btnExportToFile;

    private DatabaseHelper mDatabaseHelper;
    private List<Event> registeredList = new ArrayList<>();
    private RegisteredAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registered_events);

        mDatabaseHelper = new DatabaseHelper(this);

        btnBack = findViewById(R.id.btnBack);
        tvRegisteredSummary = findViewById(R.id.tvRegisteredSummary);
        tvEmptyRegistered = findViewById(R.id.tvEmptyRegistered);
        lvRegisteredEvents = findViewById(R.id.lvRegisteredEvents);
        btnExportToFile = findViewById(R.id.btnExportToFile);

        btnBack.setOnClickListener(v -> finish());

        adapter = new RegisteredAdapter();
        lvRegisteredEvents.setAdapter(adapter);

        lvRegisteredEvents.setOnItemClickListener((parent, view, position, id) -> {
            Event ev = registeredList.get(position);
            new AlertDialog.Builder(this)
                    .setTitle(ev.getName())
                    .setMessage("Date: " + ev.getDate() + "\nLocation: " + ev.getLocation() + "\n\n" + ev.getDescription())
                    .setPositiveButton("OK", null)
                    .setNegativeButton("Remove Registration", (dialog, which) -> {
                        boolean deleted = mDatabaseHelper.deleteEvent(ev.getId());
                        if (deleted) {
                            Toast.makeText(this, "Registration removed from SQLite", Toast.LENGTH_SHORT).show();
                            loadRegisteredEvents();
                        }
                    })
                    .show();
        });

        btnExportToFile.setOnClickListener(v -> exportEventsToFile());

        loadRegisteredEvents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRegisteredEvents();
    }

    private void loadRegisteredEvents() {
        registeredList.clear();

        Cursor cursor = mDatabaseHelper.getData();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.col1));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DATE));
                String location = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_LOCATION));
                String desc = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DESCRIPTION));

                Event event = new Event(id, name, date, "", location, desc, "Registered", "IT381");
                registeredList.add(event);
            } while (cursor.moveToNext());
            cursor.close();
        }

        adapter.notifyDataSetChanged();

        if (registeredList.isEmpty()) {
            tvEmptyRegistered.setVisibility(View.VISIBLE);
            tvRegisteredSummary.setText("No registered events found in SQLite.");
        } else {
            tvEmptyRegistered.setVisibility(View.GONE);
            tvRegisteredSummary.setText("Total Registered Events in SQLite: " + registeredList.size());
        }
    }

    private void exportEventsToFile() {
        if (registeredList.isEmpty()) {
            Toast.makeText(this, "No registered events to export", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder sb = new StringBuilder();
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        sb.append("=== SMART EVENT COMPANION: REGISTERED EVENTS SUMMARY ===\n");
        sb.append("Generated on: ").append(timeStamp).append("\n");
        sb.append("Total Events: ").append(registeredList.size()).append("\n\n");

        for (int i = 0; i < registeredList.size(); i++) {
            Event ev = registeredList.get(i);
            sb.append(i + 1).append(". ").append(ev.getName()).append("\n");
            sb.append("   Date: ").append(ev.getDate()).append("\n");
            sb.append("   Location: ").append(ev.getLocation()).append("\n");
            sb.append("   Description: ").append(ev.getDescription()).append("\n\n");
        }

        boolean written = FileHelper.writeToFile(this, FileHelper.EVENTS_SUMMARY_FILE, sb.toString());
        if (written) {
            Toast.makeText(this, "Saved to " + FileHelper.EVENTS_SUMMARY_FILE + " in internal storage!", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Failed to write file", Toast.LENGTH_SHORT).show();
        }
    }

    private class RegisteredAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return registeredList.size();
        }

        @Override
        public Object getItem(int position) {
            return registeredList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return registeredList.get(position).getId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(RegisteredEventsActivity.this).inflate(R.layout.item_event, parent, false);
            }

            Event event = registeredList.get(position);

            TextView tvTitle = convertView.findViewById(R.id.itemEventTitle);
            TextView tvCategory = convertView.findViewById(R.id.itemEventCategory);
            TextView tvDate = convertView.findViewById(R.id.itemEventDate);
            TextView tvLocation = convertView.findViewById(R.id.itemEventLocation);
            TextView tvDesc = convertView.findViewById(R.id.itemEventDescription);

            tvTitle.setText(event.getName());
            tvCategory.setText("ID: " + event.getId());
            tvDate.setText("Date: " + event.getDate());
            tvLocation.setText("Location: " + event.getLocation());
            tvDesc.setText(event.getDescription());

            return convertView;
        }
    }
}
