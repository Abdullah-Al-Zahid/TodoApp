package com.todo.app;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class TaskDetailActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private Task task;
    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;
    private boolean isTimerRunning = false;

    private TextView tvBack, tvDelete, tvTitle, tvStar, tvTimer, tvProgressPct, tvTimerLabel;
    private ImageView ivCheck;
    private ProgressBar pbTimer;
    private Button btnStartTimer, btnResetTimer;
    private LinearLayout llPriority, llDuration;
    private EditText etNotes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        db = DatabaseHelper.getInstance(this);
        int taskId = getIntent().getIntExtra("taskId", -1);
        if (taskId == -1) { finish(); return; }

        task = db.getTasksByList(
                getSharedPreferences("TodoPrefs", MODE_PRIVATE).getInt("userId", -1),
                "all_search"
        ).stream().filter(t -> t.getId() == taskId).findFirst().orElse(null);

        if (task == null) {
            loadTaskById(taskId);
        }

        initViews();
        bindTask();
    }

    private void loadTaskById(int taskId) {
        String[] lists = {"myday", "important", "work", "personal", "study"};
        int userId = getSharedPreferences("TodoPrefs", MODE_PRIVATE).getInt("userId", -1);
        for (String list : lists) {
            for (Task t : db.getTasksByList(userId, list)) {
                if (t.getId() == taskId) {
                    task = t;
                    return;
                }
            }
        }
    }

    private void initViews() {
        tvBack = findViewById(R.id.tv_back);
        tvDelete = findViewById(R.id.tv_delete);
        tvTitle = findViewById(R.id.tv_title);
        tvStar = findViewById(R.id.tv_star);
        tvTimer = findViewById(R.id.tv_timer);
        tvProgressPct = findViewById(R.id.tv_progress_pct);
        tvTimerLabel = findViewById(R.id.tv_timer_label);
        ivCheck = findViewById(R.id.iv_check);
        pbTimer = findViewById(R.id.pb_timer);
        btnStartTimer = findViewById(R.id.btn_start_timer);
        btnResetTimer = findViewById(R.id.btn_reset_timer);
        llPriority = findViewById(R.id.ll_priority);
        llDuration = findViewById(R.id.ll_duration);
        etNotes = findViewById(R.id.et_notes);
    }

    private void bindTask() {
        if (task == null) return;

        tvTitle.setText(task.getTitle());
        etNotes.setText(task.getNotes());

        // Check
        ivCheck.setBackgroundResource(task.isCompleted() ?
                R.drawable.bg_circle_checked : R.drawable.bg_circle_outline);

        // Star
        tvStar.setText(task.isStarred() ? "⭐" : "☆");
        tvStar.setTextColor(task.isStarred() ?
                Color.parseColor("#F9A825") : Color.parseColor("#A19F9D"));

        // Timer
        updateTimerUI();

        // Priority chips
        setupPriorityChips();

        // Duration chips
        setupDurationChips();

        // Listeners
        tvBack.setOnClickListener(v -> {
            saveNotes();
            finish();
        });

        tvDelete.setOnClickListener(v -> {
            stopTimer();
            db.deleteTask(task.getId());
            finish();
        });

        ivCheck.setOnClickListener(v -> {
            task.setCompleted(!task.isCompleted());
            if (task.isCompleted()) stopTimer();
            db.toggleComplete(task.getId(), task.isCompleted());
            ivCheck.setBackgroundResource(task.isCompleted() ?
                    R.drawable.bg_circle_checked : R.drawable.bg_circle_outline);
        });

        tvStar.setOnClickListener(v -> {
            task.setStarred(!task.isStarred());
            db.toggleStar(task.getId(), task.isStarred());
            tvStar.setText(task.isStarred() ? "⭐" : "☆");
            tvStar.setTextColor(task.isStarred() ?
                    Color.parseColor("#F9A825") : Color.parseColor("#A19F9D"));
        });

        btnStartTimer.setOnClickListener(v -> {
            if (isTimerRunning) stopTimer();
            else startTimer();
        });

        btnResetTimer.setOnClickListener(v -> {
            stopTimer();
            task.setElapsed(0);
            db.updateElapsed(task.getId(), 0);
            updateTimerUI();
        });
    }

    private void setupPriorityChips() {
        llPriority.removeAllViews();
        String[][] priorities = {
                {"high", "High", "#C4272A"},
                {"med", "Medium", "#C47E00"},
                {"low", "Low", "#1E7E34"},
                {"normal", "None", "#A19F9D"}
        };

        for (String[] p : priorities) {
            String id = p[0];
            String label = p[1];
            String color = p[2];

            TextView chip = new TextView(this);
            chip.setText(label);
            chip.setTextSize(12);
            chip.setPadding(28, 16, 28, 16);
            chip.setTextColor(Color.parseColor(color));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMarginEnd(8);
            chip.setLayoutParams(params);

            if (id.equals(task.getPriority())) {
                chip.setBackgroundResource(R.drawable.bg_chip_selected);
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip);
            }

            chip.setOnClickListener(v -> {
                task.setPriority(id);
                db.updateTask(task);
                setupPriorityChips();
            });

            llPriority.addView(chip);
        }
    }

    private void setupDurationChips() {
        llDuration.removeAllViews();
        int[] durations = {5, 15, 25, 30, 45, 60};

        for (int d : durations) {
            TextView chip = new TextView(this);
            chip.setText(d + "m");
            chip.setTextSize(12);
            chip.setPadding(28, 16, 28, 16);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMarginEnd(8);
            chip.setLayoutParams(params);

            if (d == task.getDuration()) {
                chip.setBackgroundResource(R.drawable.bg_chip_selected);
                chip.setTextColor(Color.parseColor("#2564CF"));
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip);
                chip.setTextColor(Color.parseColor("#605E5C"));
            }

            chip.setOnClickListener(v -> {
                stopTimer();
                task.setDuration(d);
                task.setElapsed(0);
                db.updateTask(task);
                db.updateElapsed(task.getId(), 0);
                setupDurationChips();
                updateTimerUI();
            });

            llDuration.addView(chip);
        }
    }

    private void startTimer() {
        if (task.isCompleted()) return;
        isTimerRunning = true;
        btnStartTimer.setText("⏸ Pause");
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                task.setElapsed(task.getElapsed() + 1);
                db.updateElapsed(task.getId(), task.getElapsed());
                updateTimerUI();
                if (task.getElapsed() >= task.getDuration() * 60) {
                    task.setCompleted(true);
                    db.toggleComplete(task.getId(), true);
                    ivCheck.setBackgroundResource(R.drawable.bg_circle_checked);
                    stopTimer();
                    return;
                }
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private void stopTimer() {
        isTimerRunning = false;
        timerHandler.removeCallbacks(timerRunnable);
        btnStartTimer.setText(task.getElapsed() > 0 ? "▶ Resume" : "▶ Start");
    }

    private void updateTimerUI() {
        if (task == null) return;
        tvTimerLabel.setText("TIMER — " + task.getDuration() + " MIN");
        tvTimer.setText(task.getFormattedTimeLeft());
        int pct = Math.round(task.getProgress() * 100);
        tvProgressPct.setText(pct + "%");
        pbTimer.setProgress(pct);
    }

    private void saveNotes() {
        if (task != null) {
            task.setNotes(etNotes.getText().toString().trim());
            db.updateTask(task);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveNotes();
        stopTimer();
    }
}