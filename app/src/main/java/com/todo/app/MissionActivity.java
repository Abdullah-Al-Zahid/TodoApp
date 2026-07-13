package com.todo.app;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MissionActivity extends AppCompatActivity {

    private static final String GROQ_API_KEY = "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";
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    private DatabaseHelper db;
    private int userId;
    private String currentListId;
    private OkHttpClient client = new OkHttpClient();
    private List<JSONObject> suggestions = new ArrayList<>();
    private List<Boolean> selected = new ArrayList<>();

    private TextView tvBack, tvToast, tvSelectAll;
    private EditText etMission;
    private Button btnGenerate, btnAddSelected;
    private ProgressBar progress;
    private LinearLayout llSuggestions, llSuggestionsHeader, llToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mission);

        SharedPreferences prefs = getSharedPreferences("TodoPrefs", MODE_PRIVATE);
        userId = prefs.getInt("userId", -1);
        currentListId = getIntent().getStringExtra("listId");
        if (currentListId == null) currentListId = "myday";

        db = DatabaseHelper.getInstance(this);
        initViews();
    }

    private void initViews() {
        tvBack = findViewById(R.id.tv_back);
        tvToast = findViewById(R.id.tv_toast);
        tvSelectAll = findViewById(R.id.tv_select_all);
        etMission = findViewById(R.id.et_mission);
        btnGenerate = findViewById(R.id.btn_generate);
        btnAddSelected = findViewById(R.id.btn_add_selected);
        progress = findViewById(R.id.progress);
        llSuggestions = findViewById(R.id.ll_suggestions);
        llSuggestionsHeader = findViewById(R.id.ll_suggestions_header);
        llToast = findViewById(R.id.ll_toast);

        tvBack.setOnClickListener(v -> finish());

        btnGenerate.setOnClickListener(v -> {
            String mission = etMission.getText().toString().trim();
            if (!mission.isEmpty()) generateTasks(mission);
        });

        tvSelectAll.setOnClickListener(v -> {
            for (int i = 0; i < selected.size(); i++) selected.set(i, true);
            renderSuggestions();
            updateAddButton();
        });

        btnAddSelected.setOnClickListener(v -> addSelectedTasks());
    }

    private void generateTasks(String mission) {
        showLoading(true);
        llSuggestions.removeAllViews();
        llSuggestionsHeader.setVisibility(View.GONE);
        btnAddSelected.setVisibility(View.GONE);
        suggestions.clear();
        selected.clear();

        String prompt = "You are a productivity assistant. The user has this goal: \"" + mission + "\"\n\n" +
                "Generate exactly 6 actionable to-do tasks to help them achieve it.\n" +
                "Respond ONLY with a JSON array. No explanation, no markdown, no backticks.\n" +
                "Each item must have: title (string), priority (\"high\"/\"med\"/\"low\"), " +
                "duration (number in minutes), reason (one short sentence).\n\n" +
                "Example: [{\"title\":\"Do X\",\"priority\":\"high\",\"duration\":30,\"reason\":\"Because Y\"}]";

        try {
            JSONObject message = new JSONObject();
            message.put("role", "user");
            message.put("content", prompt);

            JSONArray messages = new JSONArray();
            messages.put(message);

            JSONObject body = new JSONObject();
            body.put("model", "llama-3.3-70b-versatile");
            body.put("messages", messages);
            body.put("max_tokens", 1000);
            body.put("temperature", 0.7);

            RequestBody requestBody = RequestBody.create(
                    body.toString(),
                    MediaType.parse("application/json"));

            Request request = new Request.Builder()
                    .url(GROQ_URL)
                    .addHeader("Authorization", "Bearer " + GROQ_API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        showLoading(false);
                        showToast("Connection failed: " + e.getMessage());
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String responseBody = response.body().string();
                        JSONObject json = new JSONObject(responseBody);
                        String content = json.getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content")
                                .trim();

                        // Clean markdown
                        content = content.replace("```json", "")
                                .replace("```", "").trim();

                        JSONArray tasks = new JSONArray(content);
                        for (int i = 0; i < tasks.length(); i++) {
                            suggestions.add(tasks.getJSONObject(i));
                            selected.add(false);
                        }

                        runOnUiThread(() -> {
                            showLoading(false);
                            llSuggestionsHeader.setVisibility(View.VISIBLE);
                            renderSuggestions();
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            showLoading(false);
                            showToast("Error parsing response");
                        });
                    }
                }
            });
        } catch (Exception e) {
            showLoading(false);
            showToast("Error: " + e.getMessage());
        }
    }

    private void renderSuggestions() {
        llSuggestions.removeAllViews();
        for (int i = 0; i < suggestions.size(); i++) {
            final int index = i;
            try {
                JSONObject sug = suggestions.get(i);
                String title = sug.getString("title");
                String priority = sug.optString("priority", "normal");
                int duration = sug.optInt("duration", 25);
                String reason = sug.optString("reason", "");
                boolean isSel = selected.get(i);

                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.HORIZONTAL);
                card.setPadding(24, 24, 24, 24);
                card.setBackgroundResource(isSel ?
                        R.drawable.bg_chip_selected : R.drawable.bg_surface);

                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                cardParams.setMargins(0, 0, 0, 16);
                card.setLayoutParams(cardParams);

                // Checkbox
                TextView tvCheck = new TextView(this);
                tvCheck.setText(isSel ? "✓" : "○");
                tvCheck.setTextSize(16);
                tvCheck.setTextColor(Color.parseColor(isSel ? "#2564CF" : "#A19F9D"));
                tvCheck.setPadding(0, 0, 20, 0);

                // Content
                LinearLayout content = new LinearLayout(this);
                content.setOrientation(LinearLayout.VERTICAL);
                content.setLayoutParams(new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

                TextView tvTitle = new TextView(this);
                tvTitle.setText(title);
                tvTitle.setTextSize(14);
                tvTitle.setTextColor(Color.parseColor("#201F1E"));
                tvTitle.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);

                LinearLayout meta = new LinearLayout(this);
                meta.setOrientation(LinearLayout.HORIZONTAL);
                meta.setPadding(0, 8, 0, 0);

                TextView tvPri = new TextView(this);
                String priColor = priority.equals("high") ? "#C4272A" :
                        priority.equals("med") ? "#C47E00" : "#1E7E34";
                tvPri.setText("🚩 " + priority);
                tvPri.setTextSize(11);
                tvPri.setTextColor(Color.parseColor(priColor));
                tvPri.setPadding(0, 0, 20, 0);

                TextView tvDur = new TextView(this);
                tvDur.setText("⏱ " + duration + " min");
                tvDur.setTextSize(11);
                tvDur.setTextColor(Color.parseColor("#605E5C"));

                meta.addView(tvPri);
                meta.addView(tvDur);

                TextView tvReason = new TextView(this);
                tvReason.setText(reason);
                tvReason.setTextSize(11);
                tvReason.setTextColor(Color.parseColor("#A19F9D"));
                tvReason.setPadding(0, 6, 0, 0);
                tvReason.setTypeface(null, android.graphics.Typeface.ITALIC);

                content.addView(tvTitle);
                content.addView(meta);
                content.addView(tvReason);

                card.addView(tvCheck);
                card.addView(content);

                card.setOnClickListener(v -> {
                    selected.set(index, !selected.get(index));
                    renderSuggestions();
                    updateAddButton();
                });

                llSuggestions.addView(card);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        updateAddButton();
    }

    private void updateAddButton() {
        int count = 0;
        for (boolean s : selected) if (s) count++;
        if (count > 0) {
            btnAddSelected.setVisibility(View.VISIBLE);
            btnAddSelected.setText("+ Add " + count + " Task" + (count > 1 ? "s" : ""));
        } else {
            btnAddSelected.setVisibility(View.GONE);
        }
    }

    private void addSelectedTasks() {
        int count = 0;
        for (int i = 0; i < suggestions.size(); i++) {
            if (selected.get(i)) {
                try {
                    JSONObject sug = suggestions.get(i);
                    Task task = new Task();
                    task.setUserId(userId);
                    task.setTitle(sug.getString("title"));
                    task.setPriority(sug.optString("priority", "normal"));
                    task.setDuration(sug.optInt("duration", 25));
                    task.setListId(currentListId.equals("important") ? "myday" : currentListId);
                    task.setStarred(currentListId.equals("important"));
                    db.addTask(task);
                    count++;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        int finalCount = count;
        showToast(finalCount + " task" + (finalCount > 1 ? "s" : "") + " added!");
        suggestions.clear();
        selected.clear();
        llSuggestions.removeAllViews();
        llSuggestionsHeader.setVisibility(View.GONE);
        btnAddSelected.setVisibility(View.GONE);
        etMission.setText("");
    }

    private void showLoading(boolean show) {
        progress.setVisibility(show ? View.VISIBLE : View.GONE);
        btnGenerate.setEnabled(!show);
        btnGenerate.setAlpha(show ? 0.5f : 1f);
    }

    private void showToast(String msg) {
        tvToast.setText("✓ " + msg);
        llToast.setVisibility(View.VISIBLE);
        llToast.postDelayed(() ->
                llToast.setVisibility(View.GONE), 3000);
    }
}