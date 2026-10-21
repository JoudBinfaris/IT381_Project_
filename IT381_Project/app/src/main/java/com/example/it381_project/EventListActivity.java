package com.example.it381_project;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * EventListActivity: Retrieves, parses, and displays event data from a JSON source.
 * Loads events directly from the local JSON asset file (events.json).
 * Additional Feature: Search filtering by keyword and category.
 */
public class EventListActivity extends AppCompatActivity {

    private Button btnLoadJson;
    private EditText etSearchEvent;
    private ListView lvEvents;
    private Button btnBack;
    private TextView tvEventListStatus;

    private Button chipAll, chipWorkshop, chipConference, chipExhibition, chipSeminar;

    private List<Event> allEvents = new ArrayList<>();
    private List<Event> displayedEvents = new ArrayList<>();
    private EventAdapter eventAdapter;
    private String selectedCategory = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_list);

        btnBack = findViewById(R.id.btnBack);
        btnLoadJson = findViewById(R.id.btnLoadJson);
        etSearchEvent = findViewById(R.id.etSearchEvent);
        lvEvents = findViewById(R.id.lvEvents);
        tvEventListStatus = findViewById(R.id.tvEventListStatus);

        chipAll = findViewById(R.id.chipAll);
        chipWorkshop = findViewById(R.id.chipWorkshop);
        chipConference = findViewById(R.id.chipConference);
        chipExhibition = findViewById(R.id.chipExhibition);
        chipSeminar = findViewById(R.id.chipSeminar);

        btnBack.setOnClickListener(v -> finish());

        eventAdapter = new EventAdapter();
        lvEvents.setAdapter(eventAdapter);

        // Load Events from local JSON asset file
        btnLoadJson.setOnClickListener(v -> loadEventsFromJson());

        lvEvents.setOnItemClickListener((parent, view, position, id) -> {
            Event selected = displayedEvents.get(position);
            Intent intent = new Intent(EventListActivity.this, EventDetailActivity.class);
            intent.putExtra("event", selected);
            startActivity(intent);
        });

        etSearchEvent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterEvents(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        setupCategoryChips();

        // Load events automatically upon opening
        loadEventsFromJson();
    }

    private void loadEventsFromJson() {
        try {
            InputStream is = getAssets().open("events.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String jsonStr = new String(buffer, StandardCharsets.UTF_8);
            parseJsonString(jsonStr);
            Toast.makeText(this, "Loaded events from JSON data source", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading JSON: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void parseJsonString(String jsonString) {
        allEvents.clear();

        try {
            JSONArray array;
            if (jsonString.trim().startsWith("[")) {
                array = new JSONArray(jsonString);
            } else {
                JSONObject json = new JSONObject(jsonString);
                if (json.has("events")) {
                    array = json.getJSONArray("events");
                } else if (json.has("data")) {
                    array = json.getJSONArray("data");
                } else {
                    String firstKey = json.keys().next();
                    array = json.getJSONArray(firstKey);
                }
            }

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                int id = obj.optInt("id", i + 1);
                String name = obj.optString("name", "Event " + (i + 1));
                String date = obj.optString("date", "2026-11-01");
                String time = obj.optString("time", "10:00 AM");
                String location = obj.optString("location", "Main Hall");
                String description = obj.optString("description", "No description provided");
                String category = obj.optString("category", "General");
                String organizer = obj.optString("organizer", "Organizer");

                allEvents.add(new Event(id, name, date, time, location, description, category, organizer));
            }

            filterEvents(etSearchEvent.getText().toString());
            tvEventListStatus.setText("Available Events (" + displayedEvents.size() + " loaded):");

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "JSON Parse Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupCategoryChips() {
        View.OnClickListener chipListener = v -> {
            Button clicked = (Button) v;
            selectedCategory = clicked.getText().toString();
            if (selectedCategory.equals("Workshops")) selectedCategory = "Workshop";
            if (selectedCategory.equals("Conferences")) selectedCategory = "Conference";
            if (selectedCategory.equals("Exhibitions")) selectedCategory = "Exhibition";
            if (selectedCategory.equals("Seminars")) selectedCategory = "Seminar";

            Toast.makeText(EventListActivity.this, "Filter: " + selectedCategory, Toast.LENGTH_SHORT).show();
            filterEvents(etSearchEvent.getText().toString());
        };

        chipAll.setOnClickListener(chipListener);
        chipWorkshop.setOnClickListener(chipListener);
        chipConference.setOnClickListener(chipListener);
        chipExhibition.setOnClickListener(chipListener);
        chipSeminar.setOnClickListener(chipListener);
    }

    private void filterEvents(String query) {
        displayedEvents.clear();
        String lowerQuery = query.toLowerCase().trim();

        for (Event ev : allEvents) {
            boolean matchesCategory = selectedCategory.equals("All") || ev.getCategory().equalsIgnoreCase(selectedCategory);
            boolean matchesSearch = lowerQuery.isEmpty() ||
                    ev.getName().toLowerCase().contains(lowerQuery) ||
                    ev.getLocation().toLowerCase().contains(lowerQuery) ||
                    ev.getDescription().toLowerCase().contains(lowerQuery);

            if (matchesCategory && matchesSearch) {
                displayedEvents.add(ev);
            }
        }
        eventAdapter.notifyDataSetChanged();
        tvEventListStatus.setText("Available Events (" + displayedEvents.size() + " shown):");
    }

    private class EventAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return displayedEvents.size();
        }

        @Override
        public Object getItem(int position) {
            return displayedEvents.get(position);
        }

        @Override
        public long getItemId(int position) {
            return displayedEvents.get(position).getId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(EventListActivity.this).inflate(R.layout.item_event, parent, false);
            }

            Event event = displayedEvents.get(position);

            TextView tvTitle = convertView.findViewById(R.id.itemEventTitle);
            TextView tvCategory = convertView.findViewById(R.id.itemEventCategory);
            TextView tvDate = convertView.findViewById(R.id.itemEventDate);
            TextView tvLocation = convertView.findViewById(R.id.itemEventLocation);
            TextView tvDesc = convertView.findViewById(R.id.itemEventDescription);

            tvTitle.setText(event.getName());
            tvCategory.setText(event.getCategory());
            tvDate.setText("Date: " + event.getDate());
            tvLocation.setText("Location: " + event.getLocation());
            tvDesc.setText(event.getDescription());

            return convertView;
        }
    }
}
