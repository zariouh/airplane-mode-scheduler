package com.airplanescheduler;

public class Schedule {
    private String id;
    private int hour;
    private int minute;
    private boolean turnOn;
    private boolean enabled;
    private long alarmTime;

    public Schedule(String id, int hour, int minute, boolean turnOn, boolean enabled) {
        this.id = id;
        this.hour = hour;
        this.minute = minute;
        this.turnOn = turnOn;
        this.enabled = enabled;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public int getHour() { return hour; }
    public void setHour(int hour) { this.hour = hour; }
    public int getMinute() { return minute; }
    public void setMinute(int minute) { this.minute = minute; }
    public boolean isTurnOn() { return turnOn; }
    public void setTurnOn(boolean turnOn) { this.turnOn = turnOn; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public long getAlarmTime() { return alarmTime; }
    public void setAlarmTime(long alarmTime) { this.alarmTime = alarmTime; }

    public String getTimeString() {
        return String.format("%02d:%02d", hour, minute);
    }

    public String getActionString() {
        return turnOn ? "ON" : "OFF";
    }
}
