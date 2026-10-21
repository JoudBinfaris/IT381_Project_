# IT381 – Wireless and Mobile Computing
## Programming Assignment: Smart Event Companion

---

### Team Information
* **Course:** IT381 – Wireless and Mobile Computing
* **Project Title:** Smart Event Companion
* **Team Members:**
  1. Member 1 (Name & Student ID)
  2. Member 2 (Name & Student ID)
  3. Member 3 (Name & Student ID)
  4. Member 4 (Name & Student ID)

---

### 1. Application Description
**Smart Event Companion** is a mobile application developed in Android Studio using Java. The application is designed to support users attending events such as academic conferences, technical workshops, student exhibitions, and campus activities.

Key features include:
* **Event Browsing & Details:** Viewing available events loaded dynamically from a JSON data source, with detailed agendas, dates, locations, and descriptions.
* **Event Registration:** Registering for desired events with records permanently stored in an SQLite database.
* **Bluetooth Interaction:** Connecting directly between nearby Android devices via Bluetooth RFCOMM sockets to facilitate interaction between event organizers and attendees.
* **Attendance Tracking:** Real-time check-in where an attendee transmits their name/ID over Bluetooth to an organizer's device, recording attendance into an SQLite table.
* **File Storage & Reporting:** Exporting structured attendance and registration summaries to Android internal file storage (`.txt`) and reading saved reports.
* **Event Search & Category Filtering:** Real-time search and category filtering enabling attendees to find relevant events quickly.

---

### 2. System Architecture & Design

The application follows an Activity-based modular architecture organized into presentation, data access, and communication layers:

```
+-------------------------------------------------------------+
|                     PRESENTATION LAYER                      |
|  - MainActivity (Dashboard & Navigation Hub)                |
|  - EventListActivity (JSON Parsing, Search & Filters)       |
|  - EventDetailActivity (Event Information & Registration)   |
|  - RegisteredEventsActivity (SQLite Records Viewer)         |
|  - BluetoothAttendanceActivity (RFCOMM Server/Client UI)    |
|  - FileStorageActivity (Export & Report Viewer)             |
+-------------------------------------------------------------+
                              |
+-----------------------------+-------------------------------+
|                      DATA & SERVICES                        |
|  - DatabaseHelper (SQLiteOpenHelper: Events & Attendance)   |
|  - FileHelper (Internal Storage I/O)                        |
|  - Bluetooth RFCOMM Sockets (UUID: SPP 00001101-...)        |
|  - Local JSON Data Source (assets/events.json)              |
+-------------------------------------------------------------+
```

#### Application Activities:
1. **`MainActivity`:** Entry point of the application. Provides direct navigation to all major app sections via a clean dashboard.
2. **`EventListActivity`:** Loads event data directly from the local JSON asset (`assets/events.json`) to parse and display available events. Includes real-time keyword search and category filtering.
3. **`EventDetailActivity`:** Displays detailed event metadata and provides a one-click button to register the event into SQLite.
4. **`RegisteredEventsActivity`:** Queries and displays previously registered events from SQLite, with an option to remove registrations or export them.
5. **`BluetoothAttendanceActivity`:** Controls device Bluetooth state, lists paired devices, and manages peer-to-peer RFCOMM connection between Organizer (Server) and Attendee (Client).
6. **`FileStorageActivity`:** Exports attendance summaries and registered events to text files, and reads saved reports directly into the UI.

---

### 3. JSON Data Processing

#### Data Format
Event data is structured in JSON format containing event metadata:
```json
[
  {
    "id": 101,
    "name": "Mobile AI & Machine Learning Workshop",
    "date": "2026-11-05",
    "time": "10:00 AM",
    "location": "Auditorium A, Building 31",
    "description": "Hands-on workshop exploring on-device artificial intelligence...",
    "category": "Workshop",
    "organizer": "IT Department"
  }
]
```

#### Retrieval and Parsing Mechanism:
* **Local Asset Loading:** Performed via `loadEventsFromJson()` in `EventListActivity`. Reads the local JSON asset file (`assets/events.json`) using `getAssets().open("events.json")` into a byte buffer and decodes it using `StandardCharsets.UTF_8`.
* **JSON Parsing:** Parsed using Android's native `JSONArray` and `JSONObject` APIs:
  ```java
  JSONArray array = new JSONArray(jsonString);
  for (int i = 0; i < array.length(); i++) {
      JSONObject obj = array.getJSONObject(i);
      String name = obj.getString("name");
      String date = obj.getString("date");
      String location = obj.getString("location");
      String description = obj.getString("description");
      // Add to event list
  }
  ```
* **Automatic Population:** Event list is automatically loaded and displayed when opening the activity, and can be refreshed anytime by tapping "Load Events from JSON".

---

### 4. Bluetooth Functionality

#### Permissions Declared in `AndroidManifest.xml`:
* `android.permission.BLUETOOTH`
* `android.permission.BLUETOOTH_ADMIN`
* `android.permission.BLUETOOTH_CONNECT`
* `android.permission.BLUETOOTH_SCAN`
* `android.permission.BLUETOOTH_ADVERTISE`
* `android.permission.ACCESS_FINE_LOCATION`

#### Bluetooth Workflow:
1. **Adapter Initialization:**
   ```java
   BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
   ```
2. **Checking & Enabling Bluetooth:**
   Checks `mBluetoothAdapter.isEnabled()`. If disabled, requests enablement via:
   ```java
   Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
   startActivityForResult(enableBtIntent, 1);
   ```
3. **Paired Devices Discovery:**
   Queries paired devices using `mBluetoothAdapter.getBondedDevices()` and populates `listView1` via an `ArrayAdapter<String>`.
4. **RFCOMM Socket Communication:**
   * **Organizer Mode (`ServerThread`):** Listens on a dedicated Bluetooth server socket:
     ```java
     BluetoothServerSocket serverSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, MY_UUID);
     BluetoothSocket socket = serverSocket.accept();
     ```
     Upon accepting a connection, it reads the participant packet (`PARTICIPANT:<name>#EVENT:<event>`) and saves the attendance record directly into SQLite.
   * **Attendee Mode (`ClientThread`):** Connects to the selected organizer device:
     ```java
     BluetoothSocket socket = selectedDevice.createRfcommSocketToServiceRecord(MY_UUID);
     socket.connect();
     socket.getOutputStream().write(payload.getBytes());
     ```

---

### 5. Database Structure (SQLite)

The application implements `DatabaseHelper` extending `SQLiteOpenHelper`.

#### Tables Schema:

##### 1. `registered_events` Table
Stores events the user has registered for:
| Column Name | Data Type | Constraint | Description |
| :--- | :--- | :--- | :--- |
| `ID` | INTEGER | PRIMARY KEY AUTOINCREMENT | Unique record ID |
| `name` | TEXT | NOT NULL | Event Title |
| `date` | TEXT | | Event Date |
| `location` | TEXT | | Event Venue |
| `description` | TEXT | | Event Details |

##### 2. `attendance_table` Table
Stores participant attendance recorded via Bluetooth:
| Column Name | Data Type | Constraint | Description |
| :--- | :--- | :--- | :--- |
| `ID` | INTEGER | PRIMARY KEY AUTOINCREMENT | Unique attendance ID |
| `participant_name` | TEXT | NOT NULL | Attendee Name or Student ID |
| `event_name` | TEXT | NOT NULL | Target Event Name |
| `date_time` | TEXT | NOT NULL | Timestamp of check-in |

#### Core Database Methods:
* `addData(String item)`: Inserts item into `registered_events`.
* `getData()`: Returns `Cursor` with `SELECT * FROM registered_events`.
* `addRegisteredEvent(...)`: Inserts full event details.
* `addAttendance(...)`: Inserts participant name, event name, and timestamp.
* `getAttendanceData()`: Queries recorded attendees sorted by newest first.

---

### 6. File Storage Functionality

The application uses Android Internal Storage (`Context.openFileOutput` and `Context.openFileInput`) through `FileHelper.java`.

#### Operations Implemented:
1. **Export Attendance Summary (`attendance_summary.txt`):**
   Reads all attendance records from SQLite, formats them into a structured text report with header, timestamp, and numbered list, and writes to private storage.
2. **Export Registered Events (`events_summary.txt`):**
   Reads all registered events from SQLite and saves a clean summary file.
3. **Read and Display Report:**
   Uses `FileInputStream` and `BufferedReader` to read the file contents line-by-line and display them directly on screen in `FileStorageActivity`.

---

### 7. Additional Feature: Event Search & Category Filtering

* **Purpose:** Allows users attending multi-track events to quickly locate relevant sessions.
* **Implementation Details:**
  * **Keyword Search:** Built using an `EditText` coupled to a `TextWatcher` listener that executes real-time string matching against event titles, descriptions, and locations.
  * **Category Filtering:** Filter buttons (*All, Workshops, Conferences, Exhibitions, Seminars*) filter the list in-memory without re-fetching data.

---

### 8. Challenges & Lessons Learned
1. **Android Bluetooth Permissions (Android 12+):**
   * *Challenge:* Newer Android versions require runtime `BLUETOOTH_CONNECT` and `BLUETOOTH_SCAN` permissions in addition to location permissions.
   * *Solution:* Implemented conditional runtime permission checks based on `Build.VERSION.SDK_INT >= Build.VERSION_CODES.S`.
2. **RFCOMM Socket Threading:**
   * *Challenge:* Bluetooth socket operations (`accept()`, `connect()`, `read()`, `write()`) are blocking and cause UI freezing or ANR if executed on the main thread.
   * *Solution:* Implemented dedicated background threads with `Handler(Looper.getMainLooper())` to safely update the UI upon successful data exchange.
3. **Offline Reliability:**
   * *Challenge:* Demonstration environments may not have internet connectivity or live external server availability.
   * *Solution:* Event data is packaged locally within application assets (`assets/events.json`) and parsed locally, guaranteeing 100% reliability and instantaneous loading.

---

### 9. Team Member Contributions
| Member Name | Student ID | Primary Responsibilities |
| :--- | :--- | :--- |
| Member 1 | ID 1 | SQLite Database (`DatabaseHelper`), Models, and Registered Events Activity |
| Member 2 | ID 2 | JSON Data processing, asset loading, parsing, and Event List Activity |
| Member 3 | ID 3 | Bluetooth interaction, RFCOMM sockets, and Attendance recording |
| Member 4 | ID 4 | File Storage, Search & Category filtering, UI layouts, and Final Report |

---

### 10. Application Screenshots
*(Insert screenshots here before submission)*
* `Screenshot 1: Main Dashboard (MainActivity)`
* `Screenshot 2: Browse Events with Search & Filters (EventListActivity)`
* `Screenshot 3: Event Details & Registration (EventDetailActivity)`
* `Screenshot 4: Registered Events in SQLite (RegisteredEventsActivity)`
* `Screenshot 5: Bluetooth Paired Devices & Attendance (BluetoothAttendanceActivity)`
* `Screenshot 6: File Storage Export & Report Reader (FileStorageActivity)`
