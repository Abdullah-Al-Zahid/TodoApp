package com.todo.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private int userId;
    private TaskAdapter adapter;
    private List<Task> results = new ArrayList<>();
    private TextView tvBack, tvEmpty;
    private EditText etSearch;
    private RecyclerView rvResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        SharedPreferences prefs = getSharedPreferences("TodoPrefs", MODE_PRIVATE);
        userId = prefs.getInt("userId", -1);
        db = DatabaseHelper.getInstance(this);

        tvBack = findViewById(R.id.tv_back);
        tvEmpty = findViewById(R.id.tv_empty);
        etSearch = findViewById(R.id.et_search);
        rvResults = findViewById(R.id.rv_results);

        adapter = new TaskAdapter(this, results,
                new TaskAdapter.OnTaskClickListener() {
                    @Override public void onTaskClick(Task task) {}
                    @Override public void onCheckClick(Task task) {
                        task.setCompleted(!task.isCompleted());
                        db.toggleComplete(task.getId(), task.isCompleted());
                        adapter.updateTask(task);
                    }
                    @Override public void onStarClick(Task task) {
                        task.setStarred(!task.isStarred());
                        db.toggleStar(task.getId(), task.isStarred());
                        adapter.updateTask(task);
                    }
                    @Override public void onPlayClick(Task task) {}
                });

        rvResults.setLayoutManager(new LinearLayoutManager(this));
        rvResults.setAdapter(adapter);

        tvBack.setOnClickListener(v -> finish());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                search(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        etSearch.requestFocus();
    }

    private void search(String query) {
        if (query.isEmpty()) {
            results.clear();
            adapter.updateTasks(results);
            tvEmpty.setText("Type to search tasks");
            tvEmpty.setVisibility(View.VISIBLE);
            rvResults.setVisibility(View.GONE);
            return;
        }

        results.clear();
        results.addAll(db.searchTasks(userId, query));
        adapter.updateTasks(results);

        if (results.isEmpty()) {
            tvEmpty.setText("No tasks found");
            tvEmpty.setVisibility(View.VISIBLE);
            rvResults.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvResults.setVisibility(View.VISIBLE);
        }
    }
}