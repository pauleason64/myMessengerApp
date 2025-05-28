package com.example.simplemessenger.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class FirebaseConfigManager {
    private static final String TAG = "FirebaseConfigManager";
    private static final String PREFS_NAME = "FirebaseConfigPrefs";
    private static final String KEY_DATABASE_URL = "database_url";
    private static final String KEY_STORAGE_BUCKET = "storage_bucket";
    private static final String KEY_PROJECT_ID = "project_id";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_AUTH_DOMAIN = "auth_domain";
    private static final String KEY_MESSAGING_SENDER_ID = "messaging_sender_id";
    private static final String KEY_APP_ID = "app_id";
    
    private static FirebaseConfigManager instance;
    private final SharedPreferences prefs;
    private final Map<String, String> configMap;

    private FirebaseConfigManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        configMap = new HashMap<>();
        
        // First try to load from assets file
        try {
            loadFromAssets(context);
        } catch (Exception e) {
            Log.w(TAG, "Failed to load from assets file, will use SharedPreferences", e);
        }
        
        // Load existing values from SharedPreferences
        configMap.put(KEY_DATABASE_URL, prefs.getString(KEY_DATABASE_URL, ""));
        configMap.put(KEY_STORAGE_BUCKET, prefs.getString(KEY_STORAGE_BUCKET, ""));
        configMap.put(KEY_PROJECT_ID, prefs.getString(KEY_PROJECT_ID, ""));
        configMap.put(KEY_API_KEY, prefs.getString(KEY_API_KEY, ""));
        configMap.put(KEY_AUTH_DOMAIN, prefs.getString(KEY_AUTH_DOMAIN, ""));
        configMap.put(KEY_MESSAGING_SENDER_ID, prefs.getString(KEY_MESSAGING_SENDER_ID, ""));
        configMap.put(KEY_APP_ID, prefs.getString(KEY_APP_ID, ""));
    }

    public void loadFromAssets(Context context) throws Exception {
        try {
            AssetManager assetManager = context.getAssets();
            InputStream inputStream = assetManager.open("firebase.properties");
            Properties properties = new Properties();
            properties.load(inputStream);
            
            // Load all properties with firebase prefix
            configMap.put(KEY_DATABASE_URL, properties.getProperty("firebase.database.url", ""));
            configMap.put(KEY_STORAGE_BUCKET, properties.getProperty("firebase.storage.bucket", ""));
            configMap.put(KEY_PROJECT_ID, properties.getProperty("firebase.project.id", ""));
            configMap.put(KEY_API_KEY, properties.getProperty("firebase.api.key", ""));
            configMap.put(KEY_AUTH_DOMAIN, properties.getProperty("firebase.auth.domain", ""));
            configMap.put(KEY_MESSAGING_SENDER_ID, properties.getProperty("firebase.messaging.sender.id", ""));
            configMap.put(KEY_APP_ID, properties.getProperty("firebase.app.id", ""));
            
            // Save to SharedPreferences
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_DATABASE_URL, configMap.get(KEY_DATABASE_URL));
            editor.putString(KEY_STORAGE_BUCKET, configMap.get(KEY_STORAGE_BUCKET));
            editor.putString(KEY_PROJECT_ID, configMap.get(KEY_PROJECT_ID));
            editor.putString(KEY_API_KEY, configMap.get(KEY_API_KEY));
            editor.putString(KEY_AUTH_DOMAIN, configMap.get(KEY_AUTH_DOMAIN));
            editor.putString(KEY_MESSAGING_SENDER_ID, configMap.get(KEY_MESSAGING_SENDER_ID));
            editor.putString(KEY_APP_ID, configMap.get(KEY_APP_ID));
            editor.apply();
            Log.d(TAG, configMap.toString());
            Log.d(TAG, "Successfully loaded Firebase configuration from assets");
        } catch (IOException e) {
            throw new Exception("Failed to load firebase.properties from assets", e);
        }
    }

    public static synchronized FirebaseConfigManager getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseConfigManager(context);
        }
        return instance;
    }

    public boolean isConfigured() {
        return !configMap.get(KEY_DATABASE_URL).isEmpty() &&
               !configMap.get(KEY_STORAGE_BUCKET).isEmpty() &&
               !configMap.get(KEY_PROJECT_ID).isEmpty() &&
               !configMap.get(KEY_API_KEY).isEmpty() &&
               !configMap.get(KEY_AUTH_DOMAIN).isEmpty() &&
               !configMap.get(KEY_MESSAGING_SENDER_ID).isEmpty() &&
               !configMap.get(KEY_APP_ID).isEmpty();
    }

    public void setConfig(Map<String, String> config) {
        SharedPreferences.Editor editor = prefs.edit();
        configMap.clear();
        
        for (Map.Entry<String, String> entry : config.entrySet()) {
            String key = entry.getKey().toLowerCase();
            String value = entry.getValue();
            
            switch (key) {
                case "databaseurl":
                    editor.putString(KEY_DATABASE_URL, value);
                    configMap.put(KEY_DATABASE_URL, value);
                    break;
                case "storagebucket":
                    editor.putString(KEY_STORAGE_BUCKET, value);
                    configMap.put(KEY_STORAGE_BUCKET, value);
                    break;
                case "projectid":
                    editor.putString(KEY_PROJECT_ID, value);
                    configMap.put(KEY_PROJECT_ID, value);
                    break;
                case "apikey":
                    editor.putString(KEY_API_KEY, value);
                    configMap.put(KEY_API_KEY, value);
                    break;
                case "authdomain":
                    editor.putString(KEY_AUTH_DOMAIN, value);
                    configMap.put(KEY_AUTH_DOMAIN, value);
                    break;
                case "messagingsenderid":
                    editor.putString(KEY_MESSAGING_SENDER_ID, value);
                    configMap.put(KEY_MESSAGING_SENDER_ID, value);
                    break;
                case "appid":
                    editor.putString(KEY_APP_ID, value);
                    configMap.put(KEY_APP_ID, value);
                    break;
            }
        }
        editor.apply();
    }

    public String getDatabaseUrl() {
        return configMap.get(KEY_DATABASE_URL);
    }

    public String getStorageBucket() {
        return configMap.get(KEY_STORAGE_BUCKET);
    }

    public String getProjectId() {
        return configMap.get(KEY_PROJECT_ID);
    }

    public String getApiKey() {
        return configMap.get(KEY_API_KEY);
    }

    public String getAuthDomain() {
        return configMap.get(KEY_AUTH_DOMAIN);
    }

    public String getMessagingSenderId() {
        return configMap.get(KEY_MESSAGING_SENDER_ID);
    }

    public String getAppId() {
        return configMap.get(KEY_APP_ID);
    }

    public void clearConfig() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear().apply();
        configMap.clear();
    }

    public Map<String, String> getConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("databaseurl", getDatabaseUrl());
        config.put("storagebucket", getStorageBucket());
        config.put("projectid", getProjectId());
        config.put("apikey", getApiKey());
        config.put("authdomain", getAuthDomain());
        config.put("messagingsenderid", getMessagingSenderId());
        config.put("appid", getAppId());
        return config;
    }
}
