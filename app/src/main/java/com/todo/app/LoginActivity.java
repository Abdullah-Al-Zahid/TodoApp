package com.todo.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.LinearLayout;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etEmail, etPassword;
    private Button btnAction;
    private TextView tvTitle, tvSwitch, tvError;
    private ProgressBar progress;
    private DatabaseHelper db;
    private boolean isLoginMode = true;
    private View vS1, vS2, vS3, vS4;
    private TextView tvStrengthLabel, tvStrengthHint;
    private LinearLayout llStrength;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if already logged in
        SharedPreferences prefs = getSharedPreferences("TodoPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("userId", -1);
        if (userId != -1) {
            goToMain();
            return;
        }

        setContentView(R.layout.activity_login);
        db = DatabaseHelper.getInstance(this);
        initViews();
        setupListeners();
    }

    private void initViews() {

        etUsername = findViewById(R.id.et_username);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnAction = findViewById(R.id.btn_action);
        tvTitle = findViewById(R.id.tv_title);
        tvSwitch = findViewById(R.id.tv_switch);
        tvError = findViewById(R.id.tv_error);
        progress = findViewById(R.id.progress);
        vS1 = findViewById(R.id.v_s1);
        vS2 = findViewById(R.id.v_s2);
        vS3 = findViewById(R.id.v_s3);
        vS4 = findViewById(R.id.v_s4);
        tvStrengthLabel = findViewById(R.id.tv_strength_label);
        tvStrengthHint = findViewById(R.id.tv_strength_hint);
        llStrength = findViewById(R.id.ll_strength);
    }

    private void setupListeners() {
        etPassword.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!isLoginMode) {
                    checkPasswordStrength(s.toString());
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
        btnAction.setOnClickListener(v -> {
            if (isLoginMode) handleLogin();
            else handleRegister();
        });

        tvSwitch.setOnClickListener(v -> toggleMode());
    }

    private void handleLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }

        showLoading(true);
        try {
            User user = db.loginUser(email, password);
            showLoading(false);

            if (user != null) {
                // Save user session
                SharedPreferences.Editor editor = getSharedPreferences("TodoPrefs", MODE_PRIVATE).edit();
                editor.putInt("userId", user.getId());
                editor.putString("username", user.getUsername());
                editor.putString("email", user.getEmail());
                editor.apply();
                goToMain();
            } else {
                showError("Invalid email or password");
            }
        } catch (Exception e) {
            showLoading(false);
            showError("Login failed: " + e.getMessage());
        }
    }

    private void handleRegister() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }

        if (password.length() < 4) {
            showError("Password must be at least 4 characters");
            return;
        }

        showLoading(true);
        boolean success = db.registerUser(username, email, password);
        showLoading(false);

        if (success) {
            toggleMode();
            showError("");
            tvError.setTextColor(getResources().getColor(R.color.green));
            tvError.setText("Account created! Please sign in.");
            tvError.setVisibility(View.VISIBLE);
        } else {
            showError("Email already exists");
        }
    }

    private void toggleMode() {
        isLoginMode = !isLoginMode;
        if (isLoginMode) {
            tvTitle.setText("Sign in");
            btnAction.setText("Sign in");
            tvSwitch.setText("Don't have an account? Create one");
            etUsername.setVisibility(View.GONE);
            llStrength.setVisibility(View.GONE);
        } else {
            tvTitle.setText("Create account");
            btnAction.setText("Create account");
            tvSwitch.setText("Already have an account? Sign in");
            etUsername.setVisibility(View.VISIBLE);
        }
        tvError.setVisibility(View.GONE);
    }

    private void showError(String message) {
        if (message.isEmpty()) {
            tvError.setVisibility(View.GONE);
        } else {
            tvError.setTextColor(getResources().getColor(R.color.red));
            tvError.setText(message);
            tvError.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean show) {
        progress.setVisibility(show ? View.VISIBLE : View.GONE);
        btnAction.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
    private void checkPasswordStrength(String password) {
        if (password.isEmpty()) {
            llStrength.setVisibility(View.GONE);
            return;
        }

        llStrength.setVisibility(View.VISIBLE);

        int score = 0;
        String hint = "";

        // Length check
        if (password.length() >= 6) score++;
        if (password.length() >= 10) score++;

        // Has numbers
        if (password.matches(".*[0-9].*")) score++;

        // Has special characters
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) score++;

        // Has uppercase
        if (password.matches(".*[A-Z].*")) score++;

        // Normalize score to max 4
        if (score > 4) score = 4;

        // Reset all bars
        int gray = android.graphics.Color.parseColor("#E1DFDD");
        vS1.setBackgroundColor(gray);
        vS2.setBackgroundColor(gray);
        vS3.setBackgroundColor(gray);
        vS4.setBackgroundColor(gray);

        switch (score) {
            case 1:
                vS1.setBackgroundColor(android.graphics.Color.parseColor("#C4272A"));
                tvStrengthLabel.setText("Weak");
                tvStrengthLabel.setTextColor(
                        android.graphics.Color.parseColor("#C4272A"));
                hint = "Add numbers or symbols";
                break;
            case 2:
                vS1.setBackgroundColor(android.graphics.Color.parseColor("#FF6B35"));
                vS2.setBackgroundColor(android.graphics.Color.parseColor("#FF6B35"));
                tvStrengthLabel.setText("Fair");
                tvStrengthLabel.setTextColor(
                        android.graphics.Color.parseColor("#FF6B35"));
                hint = "Add uppercase letters";
                break;
            case 3:
                vS1.setBackgroundColor(android.graphics.Color.parseColor("#C47E00"));
                vS2.setBackgroundColor(android.graphics.Color.parseColor("#C47E00"));
                vS3.setBackgroundColor(android.graphics.Color.parseColor("#C47E00"));
                tvStrengthLabel.setText("Good");
                tvStrengthLabel.setTextColor(
                        android.graphics.Color.parseColor("#C47E00"));
                hint = "Add special characters";
                break;
            case 4:
            case 5:
                vS1.setBackgroundColor(android.graphics.Color.parseColor("#1E7E34"));
                vS2.setBackgroundColor(android.graphics.Color.parseColor("#1E7E34"));
                vS3.setBackgroundColor(android.graphics.Color.parseColor("#1E7E34"));
                vS4.setBackgroundColor(android.graphics.Color.parseColor("#1E7E34"));
                tvStrengthLabel.setText("Strong 💪");
                tvStrengthLabel.setTextColor(
                        android.graphics.Color.parseColor("#1E7E34"));
                hint = "Great password!";
                break;
            default:
                tvStrengthLabel.setText("Too short");
                tvStrengthLabel.setTextColor(
                        android.graphics.Color.parseColor("#C4272A"));
                hint = "Use at least 6 characters";
                break;
        }

        tvStrengthHint.setText(hint);
    }
}