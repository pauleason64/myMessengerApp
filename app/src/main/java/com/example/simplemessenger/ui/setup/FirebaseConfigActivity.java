package com.example.simplemessenger.ui.setup;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.simplemessenger.R;
import com.example.simplemessenger.ui.main.MainActivity;
import com.example.simplemessenger.util.FirebaseConfigManager;
import com.example.simplemessenger.util.FirebaseFactory;

import java.util.HashMap;
import java.util.Map;

public class FirebaseConfigActivity extends AppCompatActivity {
    private EditText editDatabaseUrl;
    private EditText editStorageBucket;
    private EditText editProjectId;
    private EditText editApiKey;
    private EditText editAuthDomain;
    private EditText editMessagingSenderId;
    private EditText editAppId;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firebase_config);

        // Get Firebase configuration if exists
        FirebaseConfigManager configManager = FirebaseConfigManager.getInstance(this);
        if (configManager.isConfigured()) {
            // If already configured, load contacts first
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("load_contacts_first", true);
            startActivity(intent);
            finish();
            return;
        }

        // Check if we have values from assets file
        try {
            configManager.loadFromAssets(this);
            if (configManager.isConfigured()) {
                // If we got values from assets, load contacts first
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("load_contacts_first", true);
                startActivity(intent);
                finish();
                return;
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to load from assets file", e);
        }

        // Initialize views
        editDatabaseUrl = findViewById(R.id.edit_database_url);
        editStorageBucket = findViewById(R.id.edit_storage_bucket);
        editProjectId = findViewById(R.id.edit_project_id);
        editApiKey = findViewById(R.id.edit_api_key);
        editAuthDomain = findViewById(R.id.edit_auth_domain);
        editMessagingSenderId = findViewById(R.id.edit_messaging_sender_id);
        editAppId = findViewById(R.id.edit_app_id);
        btnSave = findViewById(R.id.btn_save_config);

        // Try to load existing values
        try {
            configManager.loadFromAssets(this);
            
            // Pre-fill the input fields with existing values
            editDatabaseUrl.setText(configManager.getDatabaseUrl());
            editStorageBucket.setText(configManager.getStorageBucket());
            editProjectId.setText(configManager.getProjectId());
            editApiKey.setText(configManager.getApiKey());
            editAuthDomain.setText(configManager.getAuthDomain());
            editMessagingSenderId.setText(configManager.getMessagingSenderId());
            editAppId.setText(configManager.getAppId());
        } catch (Exception e) {
            Log.w(TAG, "Failed to load from assets file", e);
        }

        // Set click listener
        btnSave.setOnClickListener(v -> saveConfig());
    }

    private void saveConfig() {
        // Get all values
        String databaseUrl = editDatabaseUrl.getText().toString().trim();
        String storageBucket = editStorageBucket.getText().toString().trim();
        String projectId = editProjectId.getText().toString().trim();
        String apiKey = editApiKey.getText().toString().trim();
        String authDomain = editAuthDomain.getText().toString().trim();
        String messagingSenderId = editMessagingSenderId.getText().toString().trim();
        String appId = editAppId.getText().toString().trim();

        // Validate required fields
        if (databaseUrl.isEmpty()) {
            editDatabaseUrl.setError("Database URL is required");
            return;
        }
        if (projectId.isEmpty()) {
            editProjectId.setError("Project ID is required");
            return;
        }
        if (apiKey.isEmpty()) {
            editApiKey.setError("API Key is required");
            return;
        }

        // Create config map
        Map<String, String> config = new HashMap<>();
        config.put("databaseurl", databaseUrl);
        config.put("storagebucket", storageBucket);
        config.put("projectid", projectId);
        config.put("apikey", apiKey);
        config.put("authdomain", authDomain);
        config.put("messagingsenderid", messagingSenderId);
        config.put("appid", appId);

        // Save configuration
        FirebaseConfigManager.getInstance(this).setConfig(config);

        // Try to initialize Firebase
        try {
            FirebaseFactory.initialize(this);
            // If successful, start MainActivity
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            // If initialization fails, show error and keep user in config screen
            Toast.makeText(this, "Firebase initialization failed. Please check your configuration values.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Firebase initialization failed", e);
        }
    }
}
