package com.example.SImpleMessenger.util;

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
    public static final String KEY_DATABASE_URL = "database_url";
    public static final String KEY_STORAGE_BUCKET = "storage_bucket";
    public static final String KEY_PROJECT_ID = "project_id";
    public static final String KEY_API_KEY = "api_key";
    public static final String KEY_AUTH_DOMAIN = "auth_domain";
    public static final String KEY_MESSAGING_SENDER_ID = "messaging_sender_id";
    public static final String KEY_APP_ID = "app_id";
    
    private static FirebaseConfigManager instance;
    private final SharedPreferences prefs;
    private final Map<String, String> configMap;

    private FirebaseConfigManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        configMap = new HashMap<>();
        
        // First, load from SharedPreferences
        loadFromSharedPreferences();
        
        // If not fully configured, try to load from assets file
        if (!isMinimallyConfigured()) {
            try {
                loadFromAssets(context);
                // If we successfully loaded from assets, save to SharedPreferences
                saveToSharedPreferences();
            } catch (Exception e) {
                Log.w(TAG, "Failed to load from assets file", e);
            }
        }
    }
    
    private void loadFromSharedPreferences() {
        configMap.put(KEY_DATABASE_URL, prefs.getString(KEY_DATABASE_URL, ""));
        configMap.put(KEY_STORAGE_BUCKET, prefs.getString(KEY_STORAGE_BUCKET, ""));
        configMap.put(KEY_PROJECT_ID, prefs.getString(KEY_PROJECT_ID, ""));
        configMap.put(KEY_API_KEY, prefs.getString(KEY_API_KEY, ""));
        configMap.put(KEY_AUTH_DOMAIN, prefs.getString(KEY_AUTH_DOMAIN, ""));
        configMap.put(KEY_MESSAGING_SENDER_ID, prefs.getString(KEY_MESSAGING_SENDER_ID, ""));
        configMap.put(KEY_APP_ID, prefs.getString(KEY_APP_ID, ""));
    }
    
    public void saveToSharedPreferences() {
        SharedPreferences.Editor editor = prefs.edit();
        for (Map.Entry<String, String> entry : configMap.entrySet()) {
            editor.putString(entry.getKey(), entry.getValue());
        }
        editor.apply();
    }
    
    private boolean isMinimallyConfigured() {
        return !configMap.get(KEY_DATABASE_URL).isEmpty() &&
               !configMap.get(KEY_STORAGE_BUCKET).isEmpty() &&
               !configMap.get(KEY_APP_ID).isEmpty();
    }

    public void loadFromAssets(Context context) throws Exception {
        Log.d(TAG, "Attempting to load firebase.properties.new from assets");
        AssetManager assetManager = context.getAssets();
        
        // First check if the file exists
        try {
            String[] files = assetManager.list("");
            boolean fileExists = false;
            for (String file : files) {
                if (file.equals("firebase.properties.new")) {
                    fileExists = true;
                    break;
                }
            }
            
            if (!fileExists) {
                throw new Exception("firebase.properties.new not found in assets");
            }
            
            // Now read the file
            try (InputStream inputStream = assetManager.open("firebase.properties.new")) {
                Properties properties = new Properties();
                properties.load(inputStream);
                
                // Log all properties for debugging
                Log.d(TAG, "Properties found in firebase.properties.new: " + properties.toString());
                
                // Load all properties with firebase prefix
                String dbUrl = properties.getProperty("firebase.database.url", "").trim();
                String storageBucket = properties.getProperty("firebase.storage.bucket", "").trim();
                String appId = properties.getProperty("firebase.app.id", "").trim();
                
                Log.d(TAG, String.format("Loaded values - DB: %s, Bucket: %s, AppID: %s", 
                    dbUrl, storageBucket, appId));
                
                // Only update if we have the required minimal configuration
                if (!dbUrl.isEmpty() && !storageBucket.isEmpty() && !appId.isEmpty()) {
                    configMap.put(KEY_DATABASE_URL, dbUrl);
                    configMap.put(KEY_STORAGE_BUCKET, storageBucket);
                    configMap.put(KEY_APP_ID, appId);
                    
                    // Optional fields - only set if present
                    String projectId = properties.getProperty("firebase.project.id", "").trim();
                    String apiKey = properties.getProperty("firebase.api.key", "").trim();
                    String authDomain = properties.getProperty("firebase.auth.domain", "").trim();
                    String senderId = properties.getProperty("firebase.messaging.sender.id", "").trim();
                    
                    if (!projectId.isEmpty()) configMap.put(KEY_PROJECT_ID, projectId);
                    if (!apiKey.isEmpty()) configMap.put(KEY_API_KEY, apiKey);
                    if (!authDomain.isEmpty()) configMap.put(KEY_AUTH_DOMAIN, authDomain);
                    if (!senderId.isEmpty()) configMap.put(KEY_MESSAGING_SENDER_ID, senderId);
                    
                    Log.d(TAG, "Successfully loaded Firebase configuration from assets");
                    Log.d(TAG, "Config map after loading: " + configMap.toString());
                    return; // Successfully loaded from assets
                } else {
                    throw new Exception("firebase.properties.new is missing required fields (database URL, storage bucket, or app ID)");
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading firebase.properties.new", e);
            throw new Exception("Failed to read firebase.properties.new from assets: " + e.getMessage(), e);
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
