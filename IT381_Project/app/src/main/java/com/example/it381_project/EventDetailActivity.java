package com.example.it381_project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * EventDetailActivity: Displays detailed information about a selected event
 * and allows user to register for the event (persisted in SQLite Database).
 */
@SuppressWarnings("deprecation")
public class EventDetailActivity extends AppCompatActivity {

    private Button btnBack;
    private TextView tvDetailCategory, tvDetailName, tvDetailDate, tvDetailTime;
    private TextView tvDetailLocation, tvDetailOrganizer, tvDetailDescription, tvRegistrationStatus;
    private Button btnRegisterEvent, btnGoToBluetooth;

    private DatabaseHelper mDatabaseHelper;
    private Event currentEvent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        mDatabaseHelper = new DatabaseHelper(this);

        btnBack = findViewById(R.id.btnBack);
        tvDetailCategory = findViewById(R.id.tvDetailCategory);
        tvDetailName = findViewById(R.id.tvDetailName);
        tvDetailDate = findViewById(R.id.tvDetailDate);
        tvDetailTime = findViewById(R.id.tvDetailTime);
        tvDetailLocation = findViewById(R.id.tvDetailLocation);
        tvDetailOrganizer = findViewById(R.id.tvDetailOrganizer);
        tvDetailDescription = findViewById(R.id.tvDetailDescription);
        tvRegistrationStatus = findViewById(R.id.tvRegistrationStatus);
        btnRegisterEvent = findViewById(R.id.btnRegisterEvent);
        btnGoToBluetooth = findViewById(R.id.btnGoToBluetooth);

        btnBack.setOnClickListener(v -> finish());

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            currentEvent = getIntent().getSerializableExtra("event", Event.class);
        } else {
            currentEvent = (Event) getIntent().getSerializableExtra("event");
        }
        if (currentEvent == null) {
            Toast.makeText(this, "Event data not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        displayEventDetails();
        checkRegistrationStatus();

        btnRegisterEvent.setOnClickListener(v -> registerForEvent());

        btnGoToBluetooth.setOnClickListener(v -> {
            Intent intent = new Intent(EventDetailActivity.this, BluetoothAttendanceActivity.class);
            intent.putExtra("selected_event_name", currentEvent.getName());
            startActivity(intent);
        });
    }

    private void displayEventDetails() {
        tvDetailCategory.setText(currentEvent.getCategory());
        tvDetailName.setText(currentEvent.getName());
        tvDetailDate.setText("📅 Date: " + currentEvent.getDate());
        tvDetailTime.setText("⏰ Time: " + currentEvent.getTime());
        tvDetailLocation.setText("📍 Location: " + currentEvent.getLocation());
        tvDetailOrganizer.setText("🏛 Organizer: " + currentEvent.getOrganizer());
        tvDetailDescription.setText(currentEvent.getDescription());
    }

    private void checkRegistrationStatus() {
        boolean registered = mDatabaseHelper.isEventRegistered(currentEvent.getName());
        if (registered) {
            tvRegistrationStatus.setVisibility(View.VISIBLE);
            tvRegistrationStatus.setText("✓ You are registered for this event");
            btnRegisterEvent.setEnabled(false);
            btnRegisterEvent.setText("Already Registered");
        } else {
            tvRegistrationStatus.setVisibility(View.GONE);
            btnRegisterEvent.setEnabled(true);
            btnRegisterEvent.setText("Register for Event (Save to SQLite)");
        }
    }

    private void registerForEvent() {
        boolean success = mDatabaseHelper.addRegisteredEvent(
                currentEvent.getName(),
                currentEvent.getDate(),
                currentEvent.getLocation(),
                currentEvent.getDescription()
        );

        if (success) {
            Toast.makeText(this, "Registration Successful! Saved in SQLite Database.", Toast.LENGTH_LONG).show();
            checkRegistrationStatus();
        } else {
            Toast.makeText(this, "Failed to register for event.", Toast.LENGTH_SHORT).show();
        }
    }
}
