package com.todo.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 1001;
    private DatabaseHelper db;
    private SharedPreferences prefs;
    private int userId;
    private String today;
    private String photoPath = "";

    // ADD THIS NEW LAUNCHER HERE:
    private androidx.activity.result.ActivityResultLauncher<Intent> imagePickerLauncher;

    private ImageView ivPhoto;
    private TextView tvAvatarLetter, tvEmail, tvDate;
    private TextView tvBack, tvSave, tvChangePhoto;
    private EditText etName, etJournal;
    private TextView tvTotalTasks, tvCompletedTasks;
    private LinearLayout llJournals;
    private Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        prefs = getSharedPreferences("TodoPrefs", MODE_PRIVATE);
        userId = prefs.getInt("userId", -1);
        db = DatabaseHelper.getInstance(this);

        // Get today's date
        today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());

        initViews();
        loadData();
    }

    private void initViews() {
        ivPhoto = findViewById(R.id.iv_photo);
        tvAvatarLetter = findViewById(R.id.tv_avatar_letter);
        tvEmail = findViewById(R.id.tv_email);
        tvDate = findViewById(R.id.tv_date);
        tvBack = findViewById(R.id.tv_back);
        tvSave = findViewById(R.id.tv_save);
        tvChangePhoto = findViewById(R.id.tv_change_photo);
        etName = findViewById(R.id.et_name);
        etJournal = findViewById(R.id.et_journal);
        tvTotalTasks = findViewById(R.id.tv_total_tasks);
        tvCompletedTasks = findViewById(R.id.tv_completed_tasks);
        llJournals = findViewById(R.id.ll_journals);
        btnLogout = findViewById(R.id.btn_logout);

        tvBack.setOnClickListener(v -> finish());
        tvSave.setOnClickListener(v -> saveProfile());
        tvChangePhoto.setOnClickListener(v -> pickPhoto());
        btnLogout.setOnClickListener(v -> logout());
        imagePickerLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        try {
                            Uri imageUri = result.getData().getData();
                            InputStream inputStream = getContentResolver()
                                    .openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            ivPhoto.setImageBitmap(bitmap);
                            tvAvatarLetter.setVisibility(View.GONE);
                            String filename = "profile_" + userId + ".jpg";
                            java.io.FileOutputStream fos = openFileOutput(
                                    filename, MODE_PRIVATE);
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                            fos.close();
                            photoPath = getFilesDir() + "/" + filename;
                        } catch (Exception e) {
                            Toast.makeText(ProfileActivity.this,
                                    "Failed to load image",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void loadData() {
        // Load user info
        String username = prefs.getString("username", "User");
        String email = prefs.getString("email", "");

        etName.setText(username);
        tvEmail.setText(email);
        tvAvatarLetter.setText(String.valueOf(username.charAt(0)).toUpperCase());

        // Set today's date
        String displayDate = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                .format(new Date());
        tvDate.setText(displayDate);

        // Try SharedPreferences first (faster)
        String savedPhoto = prefs.getString("photoPath", "");
        if (savedPhoto.isEmpty()) {
            // Fallback to database
            savedPhoto = db.getProfilePhoto(userId);
        }
        if (savedPhoto != null && !savedPhoto.isEmpty()) {
            java.io.File imgFile = new java.io.File(savedPhoto);
            if (imgFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(savedPhoto);
                if (bitmap != null) {
                    ivPhoto.setImageBitmap(bitmap);
                    tvAvatarLetter.setVisibility(View.GONE);
                    photoPath = savedPhoto;
                }
            }
        }
        // Load stats
        tvTotalTasks.setText(String.valueOf(db.getTaskCount(userId)));
        tvCompletedTasks.setText(String.valueOf(db.getCompletedCount(userId)));

        // Load today's journal
        String journal = db.getJournal(userId, today);
        etJournal.setText(journal);

        // Load past journals
        loadPastJournals();
    }

    private void loadPastJournals() {
        llJournals.removeAllViews();
        List<String[]> journals = db.getAllJournals(userId);

        if (journals.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No journal entries yet");
            empty.setTextColor(getResources().getColor(R.color.text_hint, null));
            empty.setTextSize(13);
            llJournals.addView(empty);
            return;
        }

        for (String[] journal : journals) {
            String date = journal[0];
            String content = journal[1];

            if (date.equals(today)) continue; // skip today

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.bg_input);
            card.setPadding(24, 20, 24, 20);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 12);
            card.setLayoutParams(params);

            TextView tvJournalDate = new TextView(this);
            tvJournalDate.setText(formatDate(date));
            tvJournalDate.setTextColor(getResources().getColor(R.color.blue_primary, null));
            tvJournalDate.setTextSize(12);

            TextView tvJournalContent = new TextView(this);
            tvJournalContent.setText(content.length() > 100 ?
                    content.substring(0, 100) + "..." : content);
            tvJournalContent.setTextColor(getResources().getColor(R.color.text_secondary, null));
            tvJournalContent.setTextSize(13);
            tvJournalContent.setPadding(0, 6, 0, 0);

            card.addView(tvJournalDate);
            card.addView(tvJournalContent);
            llJournals.addView(card);
        }
    }

    private String formatDate(String dateStr) {
        try {
            SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat output = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            return output.format(input.parse(dateStr));
        } catch (Exception e) {
            return dateStr;
        }
    }

    private void saveProfile() {
        String newName = etName.getText().toString().trim();
        String journalContent = etJournal.getText().toString().trim();

        if (newName.isEmpty()) {
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save username
        boolean nameSaved = db.updateUsername(userId, newName);
        prefs.edit().putString("username", newName).apply();

        // Save photo path
        if (!photoPath.isEmpty()) {
            db.updateProfilePhoto(userId, photoPath);
            prefs.edit().putString("photoPath", photoPath).apply();
        }

        // Save journal
        if (!journalContent.isEmpty()) {
            db.saveJournal(userId, today, journalContent);
        }

        // Update avatar letter immediately
        tvAvatarLetter.setText(String.valueOf(newName.charAt(0)).toUpperCase());

        Toast.makeText(this, "✓ Profile saved!", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }


    private void pickPhoto() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{android.Manifest.permission.READ_MEDIA_IMAGES}, 100);
                return;
            }
        } else {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
                return;
            }
        }
        openGallery();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 &&
                grantResults.length > 0 &&
                grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            Toast.makeText(this,
                    "Permission needed to access photos",
                    Toast.LENGTH_SHORT).show();
        }
    }


    private void logout() {
        prefs.edit().clear().apply();
        startActivity(new Intent(this, LoginActivity.class));
        finishAffinity();
    }
}