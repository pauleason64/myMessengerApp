package com.example.SImpleMessenger.util;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.util.Properties;

public class FirebaseConfig {
    private static final String TAG = "FirebaseConfig";
    private static FirebaseConfig instance;
    private final Properties properties;

    private FirebaseConfig(Context context) {
        properties = new Properties();
        try {
            properties.load(context.getAssets().open("firebase.properties.new"));
        } catch (IOException e) {
            Log.e(TAG, "Error loading firebase.properties.new", e);
        }
    }

    public static synchronized FirebaseConfig getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseConfig(context);
        }
        return instance;
    }

    public String getDatabaseUrl() {
        return properties.getProperty("firebase.database.url", "");
    }

    public String getStorageBucket() {
        return properties.getProperty("firebase.storage.bucket", "");
    }

    public String getProjectId() {
        return properties.getProperty("firebase.project.id", "");
    }

    public String getApiKey() {
        return properties.getProperty("firebase.api.key", "");
    }

    public String getAuthDomain() {
        return properties.getProperty("firebase.auth.domain", "");
    }

    public String getMessagingSenderId() {
        return properties.getProperty("firebase.messaging.sender.id", "");
    }

    public String getAppId() {
        return properties.getProperty("firebase.app.id", "");
    }
}
