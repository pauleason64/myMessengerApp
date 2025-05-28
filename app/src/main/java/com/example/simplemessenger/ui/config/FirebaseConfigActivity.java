package com.example.simplemessenger.ui.config;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.simplemessenger.R;
import com.example.simplemessenger.util.FirebaseConfigManager;

import java.util.HashMap;
import java.util.Map;

public class FirebaseConfigActivity extends AppCompatActivity {
    private EditText etDatabaseUrl;
    private EditText etStorageBucket;
    private EditText etProjectId;
    private EditText etApiKey;
    private EditText etAuthDomain;
    private EditText etMessagingSenderId;
    private EditText etAppId;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firebase_config);

        // Initialize views
        etDatabaseUrl = findViewById(R.id.edit_database_url);
        etStorageBucket = findViewById(R.id.edit_storage_bucket);
        etProjectId = findViewById(R.id.edit_project_id);
        etApiKey = findViewById(R.id.edit_api_key);
        etAuthDomain = findViewById(R.id.edit_auth_domain);
        etMessagingSenderId = findViewById(R.id.edit_messaging_sender_id);
        etAppId = findViewById(R.id.edit_app_id);
        btnSave = findViewById(R.id.btn_save_config);

        // Set up save button
        btnSave.setOnClickListener(v -> saveConfiguration());
    }

    private void saveConfiguration() {
        // Get values from EditTexts
        String databaseUrl = etDatabaseUrl.getText().toString().trim();
        String storageBucket = etStorageBucket.getText().toString().trim();
        String projectId = etProjectId.getText().toString().trim();
        String apiKey = etApiKey.getText().toString().trim();
        String authDomain = etAuthDomain.getText().toString().trim();
        String messagingSenderId = etMessagingSenderId.getText().toString().trim();
        String appId = etAppId.getText().toString().trim();

        // Validate inputs
        if (databaseUrl.isEmpty() || storageBucket.isEmpty() || projectId.isEmpty() ||
            apiKey.isEmpty() || authDomain.isEmpty() || messagingSenderId.isEmpty() ||
            appId.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save configuration
        FirebaseConfigManager configManager = FirebaseConfigManager.getInstance(this);
        Map<String, String> config = new HashMap<>();
        config.put(FirebaseConfigManager.KEY_DATABASE_URL, databaseUrl);
        config.put(FirebaseConfigManager.KEY_STORAGE_BUCKET, storageBucket);
        config.put(FirebaseConfigManager.KEY_PROJECT_ID, projectId);
        config.put(FirebaseConfigManager.KEY_API_KEY, apiKey);
        config.put(FirebaseConfigManager.KEY_AUTH_DOMAIN, authDomain);
        config.put(FirebaseConfigManager.KEY_MESSAGING_SENDER_ID, messagingSenderId);
        config.put(FirebaseConfigManager.KEY_APP_ID, appId);

        configManager.setConfig(config);

        // Restart the app
        Toast.makeText(this, "Configuration saved. Restarting app...", Toast.LENGTH_SHORT).show();
        finishAffinity();
        startActivity(getIntent());
    }
}
