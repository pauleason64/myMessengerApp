package com.example.simplemessenger.util;

import android.content.Context;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseFactory {
    private static FirebaseAuth auth;
    private static FirebaseDatabase database;
    private static String databaseUrl;

    private FirebaseFactory() {}

    public static void initialize(Context context) {
        FirebaseConfigManager configManager = FirebaseConfigManager.getInstance(context);
        
        // Get database URL first
        databaseUrl = configManager.getDatabaseUrl();
        if (databaseUrl.isEmpty()) {
            throw new IllegalStateException("Firebase database URL not configured");
        }

        // Initialize FirebaseApp if not already initialized
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                .setApplicationId(configManager.getAppId())
                .setApiKey(configManager.getApiKey())
                .setDatabaseUrl(configManager.getDatabaseUrl())
                .setProjectId(configManager.getProjectId())
                .setStorageBucket(configManager.getStorageBucket())
                .build();
            FirebaseApp.initializeApp(context, options, "[DEFAULT]");
        }

        // Initialize Firebase components
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance(databaseUrl);
        
        // Enable disk persistence and set cache size (10MB)
        database.setPersistenceEnabled(true);
        database.setPersistenceCacheSizeBytes(10 * 1024 * 1024); // 10MB
    }

    public static FirebaseAuth getAuth() {
        if (auth == null) {
            throw new IllegalStateException("Firebase not initialized");
        }
        return auth;
    }

    public static FirebaseDatabase getDatabase() {
        if (database == null) {
            throw new IllegalStateException("Firebase not initialized");
        }
        return database;
    }

    public static String getDatabaseUrl() {
        return databaseUrl;
    }
}
