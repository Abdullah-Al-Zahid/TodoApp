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
    private static final int DB_VERSION = 1;

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
        // Create users table
        String createUsers = "CREATE TABLE " + TABLE_USERS + " (" +
                U_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                U_USERNAME + " TEXT NOT NULL, " +
                U_EMAIL + " TEXT NOT NULL UNIQUE, " +
                U_PASSWORD + " TEXT NOT NULL)";
        db.execSQL(createUsers);

        // Create tasks table
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
                T_CREATED_AT + " INTEGER DEFAULT 0)";
        db.execSQL(createTasks);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TASKS);
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
        Cursor cursor = db.query(TABLE_USERS, null,
                U_EMAIL + "=? AND " + U_PASSWORD + "=?",
                new String[]{email, password}, null, null, null);

        if (cursor.moveToFirst()) {
            User user = new User();
            user.setId(cursor.getInt(cursor.getColumnIndexOrThrow(U_ID)));
            user.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(U_USERNAME)));
            user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(U_EMAIL)));
            user.setPassword(cursor.getString(cursor.getColumnIndexOrThrow(U_PASSWORD)));
            cursor.close();
            return user;
        }
        cursor.close();
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
        return task;
    }
}