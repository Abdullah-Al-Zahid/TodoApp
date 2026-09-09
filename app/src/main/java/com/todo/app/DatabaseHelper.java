package com.todo.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "TodoApp.db";
    private static final int DB_VERSION = 6; // Incremented version to fix potential schema issues

    // Tables
    private static final String TABLE_USERS = "users";
    private static final String TABLE_TASKS = "tasks";

    // Users columns
    private static final String U_ID = "id";
    private static final String U_USERNAME = "username";
    private static final String U_EMAIL = "email";
    private static final String U_PASSWORD = "password";

    // Tasks columns
    private static final String T_ID = "id";
    private static final String T_USER_ID = "user_id";
    private static final String T_TITLE = "title";
    private static final String T_NOTES = "notes";
    private static final String T_COMPLETED = "completed";
    private static final String T_STARRED = "starred";
    private static final String T_PRIORITY = "priority";
    private static final String T_DURATION = "duration";
    private static final String T_ELAPSED = "elapsed";
    private static final String T_LIST_ID = "list_id";
    private static final String T_DUE_DATE = "due_date";
    private static final String T_CREATED_AT = "created_at";

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createUsers = "CREATE TABLE " + TABLE_USERS + " (" +
                U_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                U_USERNAME + " TEXT NOT NULL, " +
                U_EMAIL + " TEXT NOT NULL UNIQUE, " +
                U_PASSWORD + " TEXT NOT NULL, " +
                "photo_path TEXT DEFAULT '')";
        db.execSQL(createUsers);

        String createTasks = "CREATE TABLE " + TABLE_TASKS + " (" +
                T_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                T_USER_ID + " INTEGER NOT NULL, " +
                T_TITLE + " TEXT NOT NULL, " +
                T_NOTES + " TEXT DEFAULT '', " +
                T_COMPLETED + " INTEGER DEFAULT 0, " +
                T_STARRED + " INTEGER DEFAULT 0, " +
                T_PRIORITY + " TEXT DEFAULT 'normal', " +
                T_DURATION + " INTEGER DEFAULT 25, " +
                T_ELAPSED + " INTEGER DEFAULT 0, " +
                T_LIST_ID + " TEXT DEFAULT 'myday', " +
                T_DUE_DATE + " TEXT DEFAULT '', " +
                T_CREATED_AT + " INTEGER DEFAULT 0, " +
                "reminder_time INTEGER DEFAULT 0, " +
                "has_reminder INTEGER DEFAULT 0)";
        db.execSQL(createTasks);

        String createJournals = "CREATE TABLE IF NOT EXISTS journals (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER NOT NULL, " +
                "date TEXT NOT NULL, " +
                "content TEXT DEFAULT '', " +
                "updated_at INTEGER DEFAULT 0)";
        db.execSQL(createJournals);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TASKS);
        db.execSQL("DROP TABLE IF EXISTS journals");
        onCreate(db);
    }

    // ── USER METHODS ──────────────────────────────────────

    public boolean registerUser(String username, String email, String password) {
        // Check if email exists
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{U_ID},
                U_EMAIL + "=?", new String[]{email}, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.close();
            return false; // Email already exists
        }
        cursor.close();

        // Insert user
        SQLiteDatabase wdb = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(U_USERNAME, username);
        values.put(U_EMAIL, email);
        values.put(U_PASSWORD, password);
        long result = wdb.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    public User loginUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_USERS, null,
                    U_EMAIL + "=? AND " + U_PASSWORD + "=?",
                    new String[]{email, password}, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                User user = new User();
                user.setId(cursor.getInt(cursor.getColumnIndexOrThrow(U_ID)));
                user.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(U_USERNAME)));
                user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(U_EMAIL)));
                user.setPassword(cursor.getString(cursor.getColumnIndexOrThrow(U_PASSWORD)));
                return user;
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }

    public User getUserById(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null,
                U_ID + "=?", new String[]{String.valueOf(userId)},
                null, null, null);
        if (cursor.moveToFirst()) {
            User user = new User();
            user.setId(cursor.getInt(cursor.getColumnIndexOrThrow(U_ID)));
            user.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(U_USERNAME)));
            user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(U_EMAIL)));
            cursor.close();
            return user;
        }
        cursor.close();
        return null;
    }

    // ── TASK METHODS ──────────────────────────────────────

    public Task addTask(Task task) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(T_USER_ID, task.getUserId());
        values.put(T_TITLE, task.getTitle());
        values.put(T_NOTES, task.getNotes() != null ? task.getNotes() : "");
        values.put(T_COMPLETED, task.isCompleted() ? 1 : 0);
        values.put(T_STARRED, task.isStarred() ? 1 : 0);
        values.put(T_PRIORITY, task.getPriority());
        values.put(T_DURATION, task.getDuration());
        values.put(T_ELAPSED, task.getElapsed());
        values.put(T_LIST_ID, task.getListId());
        values.put(T_DUE_DATE, task.getDueDate() != null ? task.getDueDate() : "");
        values.put(T_CREATED_AT, System.currentTimeMillis());
        values.put("reminder_time", task.getReminderTime());
        values.put("has_reminder", task.isHasReminder() ? 1 : 0);
        long id = db.insert(TABLE_TASKS, null, values);
        task.setId((int) id);
        return task;
    }

    public List<Task> getTasksByList(int userId, String listId) {
        List<Task> tasks = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query;
        Cursor cursor;

        if (listId.equals("important")) {
            query = "SELECT * FROM " + TABLE_TASKS +
                    " WHERE " + T_USER_ID + "=? AND " + T_STARRED + "=1" +
                    " ORDER BY " + T_CREATED_AT + " DESC";
            cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});
        } else {
            query = "SELECT * FROM " + TABLE_TASKS +
                    " WHERE " + T_USER_ID + "=? AND " + T_LIST_ID + "=?" +
                    " ORDER BY " + T_CREATED_AT + " DESC";
            cursor = db.rawQuery(query, new String[]{String.valueOf(userId), listId});
        }

        if (cursor.moveToFirst()) {
            do {
                tasks.add(cursorToTask(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return tasks;
    }

    public List<Task> searchTasks(int userId, String query) {
        List<Task> tasks = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT * FROM " + TABLE_TASKS +
                " WHERE " + T_USER_ID + "=? AND " + T_TITLE +
                " LIKE ? ORDER BY " + T_CREATED_AT + " DESC";
        Cursor cursor = db.rawQuery(sql,
                new String[]{String.valueOf(userId), "%" + query + "%"});
        if (cursor.moveToFirst()) {
            do {
                tasks.add(cursorToTask(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return tasks;
    }

    public boolean updateTask(Task task) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(T_TITLE, task.getTitle());
        values.put(T_NOTES, task.getNotes() != null ? task.getNotes() : "");
        values.put(T_COMPLETED, task.isCompleted() ? 1 : 0);
        values.put(T_STARRED, task.isStarred() ? 1 : 0);
        values.put(T_PRIORITY, task.getPriority());
        values.put(T_DURATION, task.getDuration());
        values.put(T_ELAPSED, task.getElapsed());
        values.put(T_LIST_ID, task.getListId());
        values.put(T_DUE_DATE, task.getDueDate() != null ? task.getDueDate() : "");
        values.put("reminder_time", task.getReminderTime());
        values.put("has_reminder", task.isHasReminder() ? 1 : 0);
        int rows = db.update(TABLE_TASKS, values,
                T_ID + "=?", new String[]{String.valueOf(task.getId())});
        return rows > 0;
    }

    public boolean toggleComplete(int taskId, boolean completed) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(T_COMPLETED, completed ? 1 : 0);
        int rows = db.update(TABLE_TASKS, values,
                T_ID + "=?", new String[]{String.valueOf(taskId)});
        return rows > 0;
    }

    public boolean toggleStar(int taskId, boolean starred) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(T_STARRED, starred ? 1 : 0);
        int rows = db.update(TABLE_TASKS, values,
                T_ID + "=?", new String[]{String.valueOf(taskId)});
        return rows > 0;
    }

    public boolean updateElapsed(int taskId, int elapsed) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(T_ELAPSED, elapsed);
        int rows = db.update(TABLE_TASKS, values,
                T_ID + "=?", new String[]{String.valueOf(taskId)});
        return rows > 0;
    }

    public boolean deleteTask(int taskId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_TASKS,
                T_ID + "=?", new String[]{String.valueOf(taskId)});
        return rows > 0;
    }

    public int getTaskCount(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_TASKS +
                        " WHERE " + T_USER_ID + "=?",
                new String[]{String.valueOf(userId)});
        int count = 0;
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    public int getCompletedCount(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_TASKS +
                        " WHERE " + T_USER_ID + "=? AND " + T_COMPLETED + "=1",
                new String[]{String.valueOf(userId)});
        int count = 0;
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    private Task cursorToTask(Cursor cursor) {
        Task task = new Task();
        task.setId(cursor.getInt(cursor.getColumnIndexOrThrow(T_ID)));
        task.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow(T_USER_ID)));
        task.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(T_TITLE)));
        task.setNotes(cursor.getString(cursor.getColumnIndexOrThrow(T_NOTES)));
        task.setCompleted(cursor.getInt(cursor.getColumnIndexOrThrow(T_COMPLETED)) == 1);
        task.setStarred(cursor.getInt(cursor.getColumnIndexOrThrow(T_STARRED)) == 1);
        task.setPriority(cursor.getString(cursor.getColumnIndexOrThrow(T_PRIORITY)));
        task.setDuration(cursor.getInt(cursor.getColumnIndexOrThrow(T_DURATION)));
        task.setElapsed(cursor.getInt(cursor.getColumnIndexOrThrow(T_ELAPSED)));
        task.setListId(cursor.getString(cursor.getColumnIndexOrThrow(T_LIST_ID)));
        task.setDueDate(cursor.getString(cursor.getColumnIndexOrThrow(T_DUE_DATE)));
        task.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(T_CREATED_AT)));
        try {
            int reminderIdx = cursor.getColumnIndex("reminder_time");
            if (reminderIdx != -1) task.setReminderTime(cursor.getLong(reminderIdx));
            
            int hasReminderIdx = cursor.getColumnIndex("has_reminder");
            if (hasReminderIdx != -1) task.setHasReminder(cursor.getInt(hasReminderIdx) == 1);
        } catch (Exception e) {
            task.setReminderTime(0);
            task.setHasReminder(false);
        }
        return task;
    }
    // ── PROFILE METHODS ──────────────────────────────────────

    public boolean updateUsername(int userId, String newUsername) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(U_USERNAME, newUsername);
        int rows = db.update(TABLE_USERS, values,
                U_ID + "=?", new String[]{String.valueOf(userId)});
        return rows > 0;
    }

    public boolean updateProfilePhoto(int userId, String photoPath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("photo_path", photoPath);
        int rows = db.update(TABLE_USERS, values,
                U_ID + "=?", new String[]{String.valueOf(userId)});
        return rows > 0;
    }

    public String getProfilePhoto(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{"photo_path"},
                U_ID + "=?", new String[]{String.valueOf(userId)},
                null, null, null);
        if (cursor.moveToFirst()) {
            String path = cursor.getString(0);
            cursor.close();
            return path;
        }
        cursor.close();
        return null;
    }

// ── JOURNAL METHODS ──────────────────────────────────────

    public boolean saveJournal(int userId, String date, String content) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Check if journal for this date exists
        Cursor cursor = db.query("journals", null,
                "user_id=? AND date=?",
                new String[]{String.valueOf(userId), date},
                null, null, null);
        if (cursor.getCount() > 0) {
            cursor.close();
            ContentValues values = new ContentValues();
            values.put("content", content);
            values.put("updated_at", System.currentTimeMillis());
            int rows = db.update("journals", values,
                    "user_id=? AND date=?",
                    new String[]{String.valueOf(userId), date});
            return rows > 0;
        }
        cursor.close();
        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("date", date);
        values.put("content", content);
        values.put("updated_at", System.currentTimeMillis());
        long result = db.insert("journals", null, values);
        return result != -1;
    }

    public String getJournal(int userId, String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("journals", new String[]{"content"},
                "user_id=? AND date=?",
                new String[]{String.valueOf(userId), date},
                null, null, null);
        if (cursor.moveToFirst()) {
            String content = cursor.getString(0);
            cursor.close();
            return content;
        }
        cursor.close();
        return "";
    }

    public List<String[]> getAllJournals(int userId) {
        List<String[]> journals = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("journals",
                new String[]{"date", "content"},
                "user_id=?",
                new String[]{String.valueOf(userId)},
                null, null, "date DESC");
        if (cursor.moveToFirst()) {
            do {
                journals.add(new String[]{
                        cursor.getString(0),
                        cursor.getString(1)
                });
            } while (cursor.moveToNext());
        }
        cursor.close();
        return journals;
    }
}