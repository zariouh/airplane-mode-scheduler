package com.airplanescheduler;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements ScheduleAdapter.OnScheduleListener {

    private RecyclerView recyclerView;
    private ScheduleAdapter adapter;
    private List<Schedule> schedules;
    private SharedPreferences prefs;
    private Gson gson;
    private static final String PREFS_NAME = "AirplaneSchedulerPrefs";
    private static final String SCHEDULES_KEY = "schedules";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        
        recyclerView = findViewById(R.id.recycler_view);
        FloatingActionButton fabAddOn = findViewById(R.id.fab_add_on);
        FloatingActionButton fabAddOff = findViewById(R.id.fab_add_off);
        View btnSave = findViewById(R.id.btn_save);
        View btnLoad = findViewById(R.id.btn_load);
        View btnClear = findViewById(R.id.btn_clear);
        View btnQuickToggle = findViewById(R.id.btn_quick_toggle);

        schedules = new ArrayList<>();
        adapter = new ScheduleAdapter(schedules, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadSchedules();

        fabAddOn.setOnClickListener(v -> showTimePickerDialog(true));
        fabAddOff.setOnClickListener(v -> showTimePickerDialog(false));
        
        btnSave.setOnClickListener(v -> saveSchedules());
        btnLoad.setOnClickListener(v -> loadSchedules());
        btnClear.setOnClickListener(v -> clearAllSchedules());
        
        btnQuickToggle.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS);
            startActivity(intent);
        });

        scheduleAllAlarms();
    }

    private void showTimePickerDialog(boolean turnOn) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_time_picker, null);
        
        EditText hourInput = view.findViewById(R.id.hour_input);
        EditText minuteInput = view.findViewById(R.id.minute_input);

        builder.setView(view)
                .setTitle(turnOn ? "Add ON Time" : "Add OFF Time")
                .setPositiveButton("Add", (dialog, which) -> {
                    try {
                        int hour = Integer.parseInt(hourInput.getText().toString());
                        int minute = Integer.parseInt(minuteInput.getText().toString());
                        
                        if (hour >= 0 && hour <= 23 && minute >= 0 && minute <= 59) {
                            addSchedule(hour, minute, turnOn);
                        } else {
                            Toast.makeText(this, "Invalid time", Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addSchedule(int hour, int minute, boolean turnOn) {
        String id = UUID.randomUUID().toString();
        Schedule schedule = new Schedule(id, hour, minute, turnOn, true);
        schedules.add(schedule);
        adapter.notifyItemInserted(schedules.size() - 1);
        
        if (schedule.isEnabled()) {
            scheduleAlarm(schedule);
        }
        
        Toast.makeText(this, "Schedule added: " + schedule.getTimeString(), Toast.LENGTH_SHORT).show();
    }

    private void scheduleAlarm(Schedule schedule) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        
        Intent intent = new Intent(this, ScheduleReceiver.class);
        intent.putExtra("turn_on", schedule.isTurnOn());
        intent.putExtra("schedule_id", schedule.getId());
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, schedule.getId().hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, schedule.getHour());
        calendar.set(Calendar.MINUTE, schedule.getMinute());
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, 
                        calendar.getTimeInMillis(), 
                        pendingIntent);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, 
                    calendar.getTimeInMillis(), 
                    pendingIntent);
        } else {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP, 
                    calendar.getTimeInMillis(), 
                    pendingIntent);
        }
        
        schedule.setAlarmTime(calendar.getTimeInMillis());
    }

    private void cancelAlarm(Schedule schedule) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        
        Intent intent = new Intent(this, ScheduleReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, schedule.getId().hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        alarmManager.cancel(pendingIntent);
    }

    private void scheduleAllAlarms() {
        for (Schedule schedule : schedules) {
            if (schedule.isEnabled()) {
                scheduleAlarm(schedule);
            }
        }
    }

    private void saveSchedules() {
        String json = gson.toJson(schedules);
        prefs.edit().putString(SCHEDULES_KEY, json).apply();
        Toast.makeText(this, "Schedules saved", Toast.LENGTH_SHORT).show();
    }

    private void loadSchedules() {
        String json = prefs.getString(SCHEDULES_KEY, null);
        if (json != null) {
            Type type = new TypeToken<List<Schedule>>(){}.getType();
            List<Schedule> loaded = gson.fromJson(json, type);
            if (loaded != null) {
                schedules.clear();
                schedules.addAll(loaded);
                adapter.notifyDataSetChanged();
                scheduleAllAlarms();
            }
        }
    }

    private void clearAllSchedules() {
        for (Schedule schedule : schedules) {
            cancelAlarm(schedule);
        }
        
        schedules.clear();
        adapter.notifyDataSetChanged();
        prefs.edit().remove(SCHEDULES_KEY).apply();
        Toast.makeText(this, "All schedules cleared", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onToggleEnabled(int position, boolean enabled) {
        if (position >= 0 && position < schedules.size()) {
            Schedule schedule = schedules.get(position);
            schedule.setEnabled(enabled);
            
            if (enabled) {
                scheduleAlarm(schedule);
                Toast.makeText(this, "Alarm enabled", Toast.LENGTH_SHORT).show();
            } else {
                cancelAlarm(schedule);
                Toast.makeText(this, "Alarm disabled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDelete(int position) {
        if (position >= 0 && position < schedules.size()) {
            Schedule schedule = schedules.get(position);
            cancelAlarm(schedule);
            schedules.remove(position);
            adapter.notifyItemRemoved(position);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveSchedules();
    }
}
