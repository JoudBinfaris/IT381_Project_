package com.example.it381_project;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * BluetoothAttendanceActivity:
 * Implements Bluetooth interaction and Attendance features:
 * - Role Switcher at the top: Attendee vs Organizer
 * - Attendee Mode: Discover paired devices and transmit attendance
 * - Organizer Mode: Listen for incoming connections and save to SQLite
 */
@SuppressLint("MissingPermission")
@SuppressWarnings("deprecation")
public class BluetoothAttendanceActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BT_PERMS = 2;
    private static final String APP_NAME = "SmartEventCompanion";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // Standard SPP UUID

    BluetoothAdapter mBluetoothAdapter = null;

    // Role switcher & sections
    private RadioGroup rgRole;
    private RadioButton rbAttendee, rbOrganizer;
    private LinearLayout layoutAttendeeSection, layoutOrganizerSection;

    // Bluetooth common controls
    private TextView tvBtStatus;
    private Button btnBtOn, btnBtOff;

    // Attendee section widgets
    private Button btnGetPaired, btnSendAttendance;
    private ListView lvPairedDevices;
    private Spinner spEventSelector;
    private EditText etBtParticipant;
    private TextView tvBtLog;

    // Organizer section widgets
    private Button btnHostServer, btnRefreshAttendance;
    private TextView tvOrganizerLog;
    private ListView lvAttendanceRecords;

    private Button btnBack;

    private ArrayAdapter<String> pairedAdapter;
    private List<BluetoothDevice> pairedDeviceList = new ArrayList<>();
    private DatabaseHelper mDatabaseHelper;
    private BluetoothDevice selectedDevice = null;

    private ServerThread serverThread = null;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_attendance);

        mDatabaseHelper = new DatabaseHelper(this);

        btnBack = findViewById(R.id.btnBack);

        // Bind Role switchers and sections
        rgRole = findViewById(R.id.rgRole);
        rbAttendee = findViewById(R.id.rbAttendee);
        rbOrganizer = findViewById(R.id.rbOrganizer);
        layoutAttendeeSection = findViewById(R.id.layoutAttendeeSection);
        layoutOrganizerSection = findViewById(R.id.layoutOrganizerSection);

        // Common Bluetooth controls
        tvBtStatus = findViewById(R.id.tvBtStatus);
        btnBtOn = findViewById(R.id.btnBtOn);
        btnBtOff = findViewById(R.id.btnBtOff);

        // Attendee section
        btnGetPaired = findViewById(R.id.btnGetPaired);
        lvPairedDevices = findViewById(R.id.listView1);
        spEventSelector = findViewById(R.id.spEventSelector);
        etBtParticipant = findViewById(R.id.etBtParticipant);
        btnSendAttendance = findViewById(R.id.btnSendAttendance);
        tvBtLog = findViewById(R.id.tvBtLog);

        // Organizer section
        btnHostServer = findViewById(R.id.btnHostServer);
        tvOrganizerLog = findViewById(R.id.tvOrganizerLog);
        btnRefreshAttendance = findViewById(R.id.btnRefreshAttendance);
        lvAttendanceRecords = findViewById(R.id.lvAttendanceRecords);

        btnBack.setOnClickListener(v -> finish());

        // Setup Role Switcher logic
        rgRole.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbAttendee) {
                layoutAttendeeSection.setVisibility(View.VISIBLE);
                layoutOrganizerSection.setVisibility(View.GONE);
            } else {
                layoutAttendeeSection.setVisibility(View.GONE);
                layoutOrganizerSection.setVisibility(View.VISIBLE);
                loadAttendanceRecords();
            }
        });

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            tvBtStatus.setText("NOT SUPPORTED");
            tvBtLog.setText("Device does not support Bluetooth.");
            Toast.makeText(this, "Bluetooth not supported on this device.", Toast.LENGTH_LONG).show();
        } else {
            updateBtStatusUi();
        }

        pairedAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        lvPairedDevices.setAdapter(pairedAdapter);

        SharedPreferences prefs = getSharedPreferences("SmartEventCompanionPrefs", MODE_PRIVATE);
        String savedName = prefs.getString("participant_name", "");
        if (!savedName.isEmpty()) {
            etBtParticipant.setText(savedName);
        }

        setupEventSelector();

        btnBtOn.setOnClickListener(v -> checkAndEnableBluetooth());

        btnBtOff.setOnClickListener(v -> {
            if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
                if (hasBluetoothConnectPermission()) {
                    Toast.makeText(this, "Please turn off Bluetooth from system settings or quick panel.", Toast.LENGTH_SHORT).show();
                }
            }
            updateBtStatusUi();
        });

        btnGetPaired.setOnClickListener(v -> displayPairedDevices());

        lvPairedDevices.setOnItemClickListener((parent, view, position, id) -> {
            if (position < pairedDeviceList.size()) {
                selectedDevice = pairedDeviceList.get(position);
                String devName = hasBluetoothConnectPermission() ? selectedDevice.getName() : "Selected Device";
                tvBtLog.setText("Target: " + devName);
                Toast.makeText(this, "Selected: " + devName, Toast.LENGTH_SHORT).show();
            }
        });

        btnHostServer.setOnClickListener(v -> startOrganizerServer());
        btnSendAttendance.setOnClickListener(v -> sendAttendanceViaBluetooth());
        btnRefreshAttendance.setOnClickListener(v -> loadAttendanceRecords());

        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            displayPairedDevices();
        }
        loadAttendanceRecords();
    }

    private void updateBtStatusUi() {
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            tvBtStatus.setText("Status: ON");
            tvBtStatus.setTextColor(0xFF065F46);
        } else {
            tvBtStatus.setText("Status: OFF");
            tvBtStatus.setTextColor(0xFF991B1B);
        }
    }

    private void checkAndEnableBluetooth() {
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(BluetoothAttendanceActivity.this,
                            new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN},
                            REQUEST_BT_PERMS);
                    return;
                }
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            Toast.makeText(this, "Bluetooth is already enabled", Toast.LENGTH_SHORT).show();
            updateBtStatusUi();
            displayPairedDevices();
        }
    }

    private void displayPairedDevices() {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Please enable Bluetooth first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!hasBluetoothConnectPermission()) {
            requestBtPermissions();
            return;
        }

        pairedAdapter.clear();
        pairedDeviceList.clear();

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices != null && !pairedDevices.isEmpty()) {
            for (BluetoothDevice bt : pairedDevices) {
                String name = bt.getName() != null ? bt.getName() : "Unknown Device";
                pairedAdapter.add(name + " (" + bt.getAddress() + ")");
                pairedDeviceList.add(bt);
            }
            tvBtLog.setText("Found " + pairedDevices.size() + " paired device(s). Tap one to select.");
        } else {
            pairedAdapter.add("No paired devices found. Pair in Android Settings first.");
            tvBtLog.setText("No paired devices found.");
        }
    }

    private boolean hasBluetoothConnectPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestBtPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN},
                    REQUEST_BT_PERMS);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN},
                    REQUEST_BT_PERMS);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Bluetooth turned ON", Toast.LENGTH_SHORT).show();
                updateBtStatusUi();
                displayPairedDevices();
            } else {
                Toast.makeText(this, "Bluetooth was not enabled", Toast.LENGTH_SHORT).show();
                updateBtStatusUi();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BT_PERMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateBtStatusUi();
                displayPairedDevices();
            } else {
                Toast.makeText(this, "Bluetooth permissions are required for device interaction", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupEventSelector() {
        List<String> eventNames = new ArrayList<>();
        Cursor cursor = mDatabaseHelper.getData();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                eventNames.add(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.col1)));
            } while (cursor.moveToNext());
            cursor.close();
        }

        if (eventNames.isEmpty()) {
            eventNames.add("Mobile AI & Machine Learning Workshop");
            eventNames.add("Wireless & IoT Tech Summit");
            eventNames.add("Annual Student Tech Exhibition");
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, eventNames);
        spEventSelector.setAdapter(spinnerAdapter);

        String preSelected = getIntent().getStringExtra("selected_event_name");
        if (preSelected != null) {
            int pos = eventNames.indexOf(preSelected);
            if (pos >= 0) spEventSelector.setSelection(pos);
        }
    }

    private void startOrganizerServer() {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Please enable Bluetooth first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!hasBluetoothConnectPermission()) {
            requestBtPermissions();
            return;
        }

        if (serverThread != null) {
            serverThread.cancel();
        }

        serverThread = new ServerThread();
        serverThread.start();
        tvOrganizerLog.setText("Organizer Mode ACTIVE: Listening for nearby attendee check-ins...");
        Toast.makeText(this, "Listening for participant check-ins via Bluetooth...", Toast.LENGTH_SHORT).show();
    }

    private void sendAttendanceViaBluetooth() {
        String participant = etBtParticipant.getText().toString().trim();
        String event = spEventSelector.getSelectedItem() != null ? spEventSelector.getSelectedItem().toString() : "Event";

        if (participant.isEmpty()) {
            Toast.makeText(this, "Please enter your name / ID", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("SmartEventCompanionPrefs", MODE_PRIVATE);
        prefs.edit().putString("participant_name", participant).apply();

        if (selectedDevice == null) {
            Toast.makeText(this, "Please tap a paired device from the list first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!hasBluetoothConnectPermission()) {
            requestBtPermissions();
            return;
        }

        tvBtLog.setText("Connecting to " + selectedDevice.getName() + "...");
        new Thread(() -> {
            BluetoothSocket socket = null;
            try {
                socket = selectedDevice.createRfcommSocketToServiceRecord(MY_UUID);
                mBluetoothAdapter.cancelDiscovery();
                socket.connect();

                String payload = "PARTICIPANT:" + participant + "#EVENT:" + event + "\n";
                OutputStream out = socket.getOutputStream();
                out.write(payload.getBytes());
                out.flush();

                mainHandler.post(() -> {
                    tvBtLog.setText("Transmitted attendance successfully to " + selectedDevice.getName());
                    Toast.makeText(BluetoothAttendanceActivity.this, "Attendance sent to " + selectedDevice.getName(), Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                mainHandler.post(() -> {
                    tvBtLog.setText("Bluetooth connection failed: " + e.getMessage());
                    Toast.makeText(BluetoothAttendanceActivity.this, "Bluetooth transmission failed. Ensure Organizer device is in Organizer Mode.", Toast.LENGTH_LONG).show();
                });
            } finally {
                if (socket != null) {
                    try { socket.close(); } catch (Exception ignored) {}
                }
            }
        }).start();
    }

    private class ServerThread extends Thread {
        private BluetoothServerSocket serverSocket;
        private boolean isRunning = true;

        public ServerThread() {
            try {
                if (hasBluetoothConnectPermission()) {
                    serverSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, MY_UUID);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            while (isRunning) {
                BluetoothSocket socket = null;
                try {
                    if (serverSocket == null) break;
                    socket = serverSocket.accept();
                    if (socket != null) {
                        InputStream in = socket.getInputStream();
                        byte[] buffer = new byte[1024];
                        int bytes = in.read(buffer);
                        String msg = new String(buffer, 0, bytes);

                        String participant = "Participant";
                        String event = spEventSelector.getSelectedItem() != null ? spEventSelector.getSelectedItem().toString() : "Event";

                        if (msg.contains("PARTICIPANT:") && msg.contains("#EVENT:")) {
                            participant = msg.substring(msg.indexOf("PARTICIPANT:") + 12, msg.indexOf("#EVENT:"));
                            event = msg.substring(msg.indexOf("#EVENT:") + 7).trim();
                        } else if (!msg.isEmpty()) {
                            participant = msg.trim();
                        }

                        final String finalParticipant = participant;
                        final String finalEvent = event;

                        mainHandler.post(() -> {
                            String dateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
                            mDatabaseHelper.addAttendance(finalParticipant, finalEvent, dateTime);
                            loadAttendanceRecords();
                            tvOrganizerLog.setText("Checked in attendee: " + finalParticipant + " for " + finalEvent);
                            Toast.makeText(BluetoothAttendanceActivity.this, "Recorded Attendance: " + finalParticipant, Toast.LENGTH_SHORT).show();
                        });

                        socket.close();
                    }
                } catch (Exception e) {
                    break;
                }
            }
        }

        public void cancel() {
            isRunning = false;
            try {
                if (serverSocket != null) serverSocket.close();
            } catch (Exception ignored) {}
        }
    }

    private void loadAttendanceRecords() {
        List<Map<String, String>> data = new ArrayList<>();
        Cursor cursor = mDatabaseHelper.getAttendanceData();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Map<String, String> item = new HashMap<>();
                String pName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PARTICIPANT_NAME));
                String eName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EVENT_NAME));
                String dTime = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DATE_TIME));

                item.put("line1", "👤 " + pName + " — " + eName);
                item.put("line2", "⏰ " + dTime);
                data.add(item);
            } while (cursor.moveToNext());
            cursor.close();
        }

        SimpleAdapter adapter = new SimpleAdapter(
                this,
                data,
                android.R.layout.simple_list_item_2,
                new String[]{"line1", "line2"},
                new int[]{android.R.id.text1, android.R.id.text2}
        );
        lvAttendanceRecords.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serverThread != null) {
            serverThread.cancel();
        }
    }
}
