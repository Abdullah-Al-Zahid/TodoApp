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

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etEmail, etPassword;
    private Button btnAction;
    private TextView tvTitle, tvSwitch, tvError;
    private ProgressBar progress;
    private DatabaseHelper db;
    private boolean isLoginMode = true;

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
    }

    private void setupListeners() {
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
            showError("");
            tvError.setTextColor(getResources().getColor(R.color.green));
            tvError.setText("Account created! Please sign in.");
            tvError.setVisibility(View.VISIBLE);
            toggleMode();
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
}