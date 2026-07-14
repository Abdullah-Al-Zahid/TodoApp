package com.todo.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private SharedPreferences prefs;
    private int userId;
    private String username;
    private String currentListId = "myday";
    private int selectedDuration = 25;
    private String selectedPriority = "normal";
    private int activeTimerTaskId = -1;
    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;

    private TextView tvListTitle, tvAvatar, tvAiPlan, tvSearch, tvUserInfo, tvLogout;
    private EditText etAddTask;
    private Button btnAdd;
    private LinearLayout llLists, llDurationChips, llSuggestions, llSuggestionsContainer, llEmpty;
    private RecyclerView rvTasks;
    private TaskAdapter adapter;
    private List<Task> taskList = new ArrayList<>();

    private final String[][] LISTS = {
            {"myday", "☀️ My Day"},
            {"important", "⭐ Important"},
            {"work", "💼 Work"},
            {"personal", "🏠 Personal"},
            {"study", "📚 Study"}
    };

    private final String[] SUGGESTIONS = {
            "Review today's notes",
            "Complete assignment",
            "Read for 30 minutes",
            "Practice coding",
            "Plan tomorrow's schedule",
            "Exercise for 20 minutes",
            "Watch a tutorial",
            "Drink 8 glasses of water",
            "Take a short walk",
            "Revise last week's work"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("TodoPrefs", MODE_PRIVATE);
        userId = prefs.getInt("userId", -1);
        username = prefs.getString("username", "User");

        if (userId == -1) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        db = DatabaseHelper.getInstance(this);
        initViews();
        setupListNav();
        setupDurationChips();
        setupAddTask();
        setupSuggestions();
        loadTasks();
    }

    private void initViews() {
        tvListTitle = findViewById(R.id.tv_list_title);
        tvAvatar = findViewById(R.id.tv_avatar);
        tvAiPlan = findViewById(R.id.tv_ai_plan);
        tvSearch = findViewById(R.id.tv_search);
        tvUserInfo = findViewById(R.id.tv_user_info);
        tvLogout = findViewById(R.id.tv_logout);
        etAddTask = findViewById(R.id.et_add_task);
        btnAdd = findViewById(R.id.btn_add);
        llLists = findViewById(R.id.ll_lists);
        llDurationChips = findViewById(R.id.ll_duration_chips);
        llSuggestions = findViewById(R.id.ll_suggestions);
        llSuggestionsContainer = findViewById(R.id.ll_suggestions_container);
        llEmpty = findViewById(R.id.ll_empty);
        rvTasks = findViewById(R.id.rv_tasks);

        // Set avatar
        tvAvatar.setText(String.valueOf(username.charAt(0)).toUpperCase());
        tvUserInfo.setText("👤 " + username);

        // Setup RecyclerView
        adapter = new TaskAdapter(this, taskList, new TaskAdapter.OnTaskClickListener() {
            @Override
            public void onTaskClick(Task task) {
                openTaskDetail(task);
            }
            @Override
            public void onCheckClick(Task task) {
                toggleTaskComplete(task);
            }
            @Override
            public void onStarClick(Task task) {
                toggleTaskStar(task);
            }
            @Override
            public void onPlayClick(Task task) {
                handleTimer(task);
            }
        });
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        rvTasks.setAdapter(adapter);

        // Logout
        tvLogout.setOnClickListener(v -> logout());
        tvAvatar.setOnClickListener(v -> {
            startActivityForResult(
                    new Intent(this, ProfileActivity.class), 200);
        });

        // AI Plan
        tvAiPlan.setOnClickListener(v -> {
            Intent intent = new Intent(this, MissionActivity.class);
            intent.putExtra("listId", currentListId);
            startActivity(intent);
        });

        // Search
        tvSearch.setOnClickListener(v ->
                startActivity(new Intent(this, SearchActivity.class)));
    }

    private void setupListNav() {
        llLists.removeAllViews();
        for (String[] list : LISTS) {
            String listId = list[0];
            String listName = list[1];

            TextView chip = new TextView(this);
            chip.setText(listName);
            chip.setTextSize(13);
            chip.setPadding(40, 20, 40, 20);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMarginEnd(8);
            chip.setLayoutParams(params);

            if (listId.equals(currentListId)) {
                chip.setBackgroundResource(R.drawable.bg_chip_selected);
                chip.setTextColor(Color.parseColor("#2564CF"));
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip);
                chip.setTextColor(Color.parseColor("#605E5C"));
            }

            chip.setOnClickListener(v -> {
                currentListId = listId;
                updateListTitle(listName);
                setupListNav();
                loadTasks();
            });

            llLists.addView(chip);
        }
    }

    private void updateListTitle(String name) {
        tvListTitle.setText(name);
    }

    private void setupDurationChips() {
        llDurationChips.removeAllViews();
        int[] durations = {5, 15, 25, 30, 60};
        for (int d : durations) {
            TextView chip = new TextView(this);
            chip.setText("⏱ " + d + "m");
            chip.setTextSize(12);
            chip.setPadding(28, 14, 28, 14);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMarginEnd(6);
            chip.setLayoutParams(params);

            if (d == selectedDuration) {
                chip.setBackgroundResource(R.drawable.bg_chip_selected);
                chip.setTextColor(Color.parseColor("#2564CF"));
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip);
                chip.setTextColor(Color.parseColor("#605E5C"));
            }

            chip.setOnClickListener(v -> {
                selectedDuration = d;
                setupDurationChips();
            });

            llDurationChips.addView(chip);
        }

        // Priority chips
        String[][] priorities = {{"high", "🚩 High"}, {"med", "🚩 Med"}, {"low", "🚩 Low"}};
        String[] priColors = {"#C4272A", "#C47E00", "#1E7E34"};

        for (int i = 0; i < priorities.length; i++) {
            String priId = priorities[i][0];
            String priLabel = priorities[i][1];
            String priColor = priColors[i];

            TextView chip = new TextView(this);
            chip.setText(priLabel);
            chip.setTextSize(12);
            chip.setPadding(28, 14, 28, 14);
            chip.setTextColor(Color.parseColor(priColor));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMarginEnd(6);
            chip.setLayoutParams(params);

            if (priId.equals(selectedPriority)) {
                chip.setBackgroundResource(R.drawable.bg_chip_selected);
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip);
            }

            chip.setOnClickListener(v -> {
                selectedPriority = selectedPriority.equals(priId) ? "normal" : priId;
                setupDurationChips();
            });

            llDurationChips.addView(chip);
        }
    }

    private void setupAddTask() {
        etAddTask.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnAdd.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        etAddTask.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addTask(etAddTask.getText().toString().trim());
                return true;
            }
            return false;
        });

        btnAdd.setOnClickListener(v ->
                addTask(etAddTask.getText().toString().trim()));
    }

    private void setupSuggestions() {
        llSuggestions.removeAllViews();
        List<String> taskTitles = new ArrayList<>();
        for (Task t : taskList) taskTitles.add(t.getTitle().toLowerCase());

        boolean hasSuggestions = false;
        for (String sug : SUGGESTIONS) {
            if (!taskTitles.contains(sug.toLowerCase())) {
                hasSuggestions = true;
                TextView chip = new TextView(this);
                chip.setText("+ " + sug);
                chip.setTextSize(12);
                chip.setTextColor(Color.parseColor("#2564CF"));
                chip.setPadding(28, 16, 28, 16);
                chip.setBackgroundResource(R.drawable.bg_chip_selected);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMarginEnd(8);
                chip.setLayoutParams(params);

                chip.setOnClickListener(v -> addTask(sug));
                llSuggestions.addView(chip);
            }
        }
        llSuggestionsContainer.setVisibility(hasSuggestions ? View.VISIBLE : View.GONE);
    }

    private void addTask(String title) {
        if (title.isEmpty()) return;
        Task task = new Task();
        task.setUserId(userId);
        task.setTitle(title);
        task.setDuration(selectedDuration);
        task.setPriority(selectedPriority);
        task.setListId(currentListId.equals("important") ? "myday" : currentListId);
        task.setStarred(currentListId.equals("important"));
        Task saved = db.addTask(task);
        taskList.add(0, saved);
        adapter.notifyItemInserted(0);
        rvTasks.scrollToPosition(0);
        etAddTask.setText("");
        btnAdd.setVisibility(View.GONE);
        selectedPriority = "normal";
        setupDurationChips();
        setupSuggestions();
        updateEmptyState();
    }

    private void loadTasks() {
        taskList.clear();
        taskList.addAll(db.getTasksByList(userId, currentListId));
        adapter.updateTasks(taskList);
        updateEmptyState();
        setupSuggestions();
    }

    private void toggleTaskComplete(Task task) {
        task.setCompleted(!task.isCompleted());
        if (task.isCompleted() && activeTimerTaskId == task.getId()) {
            stopTimer();
        }
        db.toggleComplete(task.getId(), task.isCompleted());
        adapter.updateTask(task);
    }

    private void toggleTaskStar(Task task) {
        task.setStarred(!task.isStarred());
        db.toggleStar(task.getId(), task.isStarred());
        adapter.updateTask(task);
    }

    private void handleTimer(Task task) {
        if (task.isCompleted()) return;
        if (activeTimerTaskId == task.getId()) {
            stopTimer();
        } else {
            if (activeTimerTaskId != -1) stopTimer();
            startTimer(task);
        }
    }

    private void startTimer(Task task) {
        activeTimerTaskId = task.getId();
        adapter.setActiveTimerTaskId(activeTimerTaskId);
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                Task current = findTaskById(activeTimerTaskId);
                if (current == null) return;
                current.setElapsed(current.getElapsed() + 1);
                db.updateElapsed(current.getId(), current.getElapsed());
                if (current.getElapsed() >= current.getDuration() * 60) {
                    current.setCompleted(true);
                    db.toggleComplete(current.getId(), true);
                    stopTimer();
                }
                adapter.updateTask(current);
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private void stopTimer() {
        timerHandler.removeCallbacks(timerRunnable);
        activeTimerTaskId = -1;
        adapter.setActiveTimerTaskId(-1);
    }

    private Task findTaskById(int id) {
        for (Task t : taskList) {
            if (t.getId() == id) return t;
        }
        return null;
    }

    private void openTaskDetail(Task task) {
        stopTimer();
        Intent intent = new Intent(this, TaskDetailActivity.class);
        intent.putExtra("taskId", task.getId());
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            loadTasks();
        }
        if (requestCode == 200) {
            username = prefs.getString("username", "User");
            tvAvatar.setText(String.valueOf(username.charAt(0)).toUpperCase());
            tvUserInfo.setText("👤 " + username);

            // Load profile photo on avatar
            String photoPath = prefs.getString("photoPath", "");
            if (!photoPath.isEmpty()) {
                java.io.File imgFile = new java.io.File(photoPath);
                if (imgFile.exists()) {
                    android.graphics.Bitmap bitmap =
                            android.graphics.BitmapFactory.decodeFile(photoPath);
                    if (bitmap != null) {
                        // Scale bitmap to fit avatar
                        android.graphics.Bitmap scaled =
                                android.graphics.Bitmap.createScaledBitmap(
                                        bitmap, 100, 100, true);
                        android.graphics.drawable.BitmapDrawable drawable =
                                new android.graphics.drawable.BitmapDrawable(
                                        getResources(), scaled);
                        tvAvatar.setBackground(drawable);
                        tvAvatar.setText("");
                    }
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTasks();
    }

    private void updateEmptyState() {
        if (taskList.isEmpty()) {
            llEmpty.setVisibility(View.VISIBLE);
            rvTasks.setVisibility(View.GONE);
        } else {
            llEmpty.setVisibility(View.GONE);
            rvTasks.setVisibility(View.VISIBLE);
        }
    }

    private void logout() {
        stopTimer();
        prefs.edit().clear().apply();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
    }
}