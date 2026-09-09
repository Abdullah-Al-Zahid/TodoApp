package com.todo.app;

public class Task {
    private int id;
    private int userId;
    private String title;
    private String notes;
    private boolean completed;
    private boolean starred;
    private String priority;
    private int duration;
    private int elapsed;
    private String listId;
    private String dueDate;
    private long createdAt;
    private long reminderTime; // stores reminder timestamp
    private boolean hasReminder; // does this task have a reminder?

    public Task() {
        this.priority = "normal";
        this.duration = 25;
        this.elapsed = 0;
        this.completed = false;
        this.starred = false;
        this.listId = "myday";
        this.createdAt = System.currentTimeMillis();
    }


    public long getReminderTime() { return reminderTime; }
    public boolean isHasReminder() { return hasReminder; }
    public void setReminderTime(long reminderTime) { this.reminderTime = reminderTime; }
    public void setHasReminder(boolean hasReminder) { this.hasReminder = hasReminder; }
    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getNotes() { return notes; }
    public boolean isCompleted() { return completed; }
    public boolean isStarred() { return starred; }
    public String getPriority() { return priority; }
    public int getDuration() { return duration; }
    public int getElapsed() { return elapsed; }
    public String getListId() { return listId; }
    public String getDueDate() { return dueDate; }
    public long getCreatedAt() { return createdAt; }

    public void setId(int id) { this.id = id; }
    public void setUserId(int userId) { this.userId = userId; }
    public void setTitle(String title) { this.title = title; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public void setStarred(boolean starred) { this.starred = starred; }
    public void setPriority(String priority) { this.priority = priority; }
    public void setDuration(int duration) { this.duration = duration; }
    public void setElapsed(int elapsed) { this.elapsed = elapsed; }
    public void setListId(String listId) { this.listId = listId; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public int getTimeLeftSeconds() {
        return Math.max((duration * 60) - elapsed, 0);
    }

    public float getProgress() {
        if (duration == 0) return 0;
        return Math.min((float) elapsed / (duration * 60), 1f);
    }

    public String getFormattedTimeLeft() {
        int secs = getTimeLeftSeconds();
        int m = secs / 60;
        int s = secs % 60;
        return String.format("%02d:%02d", m, s);
    }

    public String getPriorityColor() {
        switch (priority) {
            case "high": return "#C4272A";
            case "med": return "#C47E00";
            case "low": return "#1E7E34";
            default: return "#A19F9D";
        }
    }

    public String getPriorityLabel() {
        switch (priority) {
            case "high": return "High";
            case "med": return "Medium";
            case "low": return "Low";
            default: return "None";
        }
    }
}