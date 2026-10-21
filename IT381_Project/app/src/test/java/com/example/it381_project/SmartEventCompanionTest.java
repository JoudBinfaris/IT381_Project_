package com.example.it381_project;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

public class SmartEventCompanionTest {

    @Test
    public void testEventModel() {
        Event event = new Event(
                1,
                "Mobile AI Workshop",
                "2026-11-05",
                "10:00 AM",
                "Hall A",
                "Cutting edge AI seminar",
                "Workshop",
                "IT Department"
        );

        assertEquals(1, event.getId());
        assertEquals("Mobile AI Workshop", event.getName());
        assertEquals("2026-11-05", event.getDate());
        assertEquals("10:00 AM", event.getTime());
        assertEquals("Hall A", event.getLocation());
        assertEquals("Cutting edge AI seminar", event.getDescription());
        assertEquals("Workshop", event.getCategory());
        assertEquals("IT Department", event.getOrganizer());
    }

    @Test
    public void testAttendanceRecordModel() {
        AttendanceRecord record = new AttendanceRecord(10, "Alice Smith", "Mobile AI Workshop", "2026-11-05 10:15");
        assertEquals(10, record.getId());
        assertEquals("Alice Smith", record.getParticipantName());
        assertEquals("Mobile AI Workshop", record.getEventName());
        assertEquals("2026-11-05 10:15", record.getDateTime());
    }

    @Test
    public void testJsonParsingStructure() throws Exception {
        String jsonSample = "[\n" +
                "  {\"id\": 1, \"name\": \"Mobile AI Workshop\", \"date\": \"2026-11-05\", \"location\": \"Hall A\", \"description\": \"AI description\"},\n" +
                "  {\"id\": 2, \"name\": \"IoT Summit\", \"date\": \"2026-11-12\", \"location\": \"Hall B\", \"description\": \"IoT description\"}\n" +
                "]";

        JSONArray array = new JSONArray(jsonSample);
        assertEquals(2, array.length());

        JSONObject first = array.getJSONObject(0);
        assertEquals("Mobile AI Workshop", first.getString("name"));
        assertEquals("2026-11-05", first.getString("date"));
        assertEquals("Hall A", first.getString("location"));
        assertEquals("AI description", first.getString("description"));

        // Test tab format string concatenation
        String module = "";
        for (int i = 0; i < array.length(); i++) {
            module += array.getJSONObject(i).getString("name");
            module += "\t\t\t";
            module += array.getJSONObject(i).getString("date");
            module += "\n";
        }

        assertTrue(module.contains("Mobile AI Workshop\t\t\t2026-11-05\n"));
        assertTrue(module.contains("IoT Summit\t\t\t2026-11-12\n"));
    }

    @Test
    public void testDatabaseConstants() {
        assertEquals("registered_events", DatabaseHelper.TABLE_NAME);
        assertEquals("name", DatabaseHelper.col1);
        assertEquals("attendance_table", DatabaseHelper.TABLE_ATTENDANCE);
    }
}
