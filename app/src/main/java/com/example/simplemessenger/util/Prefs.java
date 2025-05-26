package com.example.simplemessenger.util;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.simplemessenger.SimpleMessengerApp;
import com.google.gson.Gson;

/**
 * A utility class for managing application preferences using SharedPreferences.
 * This class provides a type-safe way to store and retrieve various data types.
 */
public class Prefs {

    private static final String PREFS_NAME = "SimpleMessengerPrefs";
    private static Prefs instance;
    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    // Preference keys
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_PHOTO = "user_photo";
    private static final String KEY_REMEMBER_ME = "remember_me";
    private static final String KEY_LAST_SYNC = "last_sync";
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    private static final String KEY_NOTIFICATION_SOUND = "notification_sound";
    private static final String KEY_NOTIFICATION_VIBRATE = "notification_vibrate";
    private static final String KEY_THEME = "app_theme";
    private static final String KEY_FIRST_LAUNCH = "first_launch";
    private static final String KEY_LAST_ACTIVE = "last_active";
    private static final String KEY_APP_VERSION = "app_version";
    private static final String KEY_FCM_TOKEN = "fcm_token";

    private Prefs(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Initialize the Prefs singleton instance.
     * Should be called in the Application class.
     */
    public static void init(Context context) {
        if (instance == null) {
            synchronized (Prefs.class) {
                if (instance == null) {
                    instance = new Prefs(context);
                }
            }
        }
    }

    /**
     * Get the singleton instance of Prefs.
     *
     * @return The Prefs instance.
     * @throws IllegalStateException if init() hasn't been called.
     */
    public static Prefs getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Prefs is not initialized. Call init() in your Application class.");
        }
        return instance;
    }

    // User-related preferences
    public void setUserId(String userId) {
        putString(KEY_USER_ID, userId);
    }

    @Nullable
    public String getUserId() {
        return getString(KEY_USER_ID, null);
    }

    public void setUserEmail(String email) {
        putString(KEY_USER_EMAIL, email);
    }

    @Nullable
    public String getUserEmail() {
        return getString(KEY_USER_EMAIL, null);
    }

    public void setUserName(String name) {
        putString(KEY_USER_NAME, name);
    }

    @Nullable
    public String getUserName() {
        return getString(KEY_USER_NAME, null);
    }

    public void setUserPhotoUrl(String photoUrl) {
        putString(KEY_USER_PHOTO, photoUrl);
    }

    @Nullable
    public String getUserPhotoUrl() {
        return getString(KEY_USER_PHOTO, null);
    }

    public void setRememberMe(boolean remember) {
        putBoolean(KEY_REMEMBER_ME, remember);
    }

    public boolean isRememberMe() {
        return getBoolean(KEY_REMEMBER_ME, false);
    }

    // App settings
    public void setLastSync(long timestamp) {
        putLong(KEY_LAST_SYNC, timestamp);
    }

    public long getLastSync() {
        return getLong(KEY_LAST_SYNC, 0);
    }

    public void setNotificationsEnabled(boolean enabled) {
        putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled);
    }

    public boolean isNotificationsEnabled() {
        return getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
    }

    public void setNotificationSound(String soundUri) {
        putString(KEY_NOTIFICATION_SOUND, soundUri);
    }

    @Nullable
    public String getNotificationSound() {
        return getString(KEY_NOTIFICATION_SOUND, null);
    }

    public void setNotificationVibrate(boolean vibrate) {
        putBoolean(KEY_NOTIFICATION_VIBRATE, vibrate);
    }

    public boolean isNotificationVibrate() {
        return getBoolean(KEY_NOTIFICATION_VIBRATE, true);
    }

    public void setTheme(String theme) {
        putString(KEY_THEME, theme);
    }

    @Nullable
    public String getTheme() {
        return getString(KEY_THEME, "system");
    }

    public void setFirstLaunch(boolean firstLaunch) {
        putBoolean(KEY_FIRST_LAUNCH, firstLaunch);
    }

    public boolean isFirstLaunch() {
        return getBoolean(KEY_FIRST_LAUNCH, true);
    }

    public void setLastActive(long timestamp) {
        putLong(KEY_LAST_ACTIVE, timestamp);
    }

    public long getLastActive() {
        return getLong(KEY_LAST_ACTIVE, 0);
    }

    public void setAppVersion(int version) {
        putInt(KEY_APP_VERSION, version);
    }

    public int getAppVersion() {
        return getInt(KEY_APP_VERSION, 0);
    }

    public void setFcmToken(String token) {
        putString(KEY_FCM_TOKEN, token);
    }

    @Nullable
    public String getFcmToken() {
        return getString(KEY_FCM_TOKEN, null);
    }

    // Helper methods for different data types
    private void putString(String key, @Nullable String value) {
        SharedPreferences.Editor editor = prefs.edit();
        if (value == null) {
            editor.remove(key);
        } else {
            editor.putString(key, value);
        }
        editor.apply();
    }

    private String getString(String key, @Nullable String defValue) {
        return prefs.getString(key, defValue);
    }

    private void putInt(String key, int value) {
        prefs.edit().putInt(key, value).apply();
    }

    private int getInt(String key, int defValue) {
        return prefs.getInt(key, defValue);
    }

    private void putLong(String key, long value) {
        prefs.edit().putLong(key, value).apply();
    }

    private long getLong(String key, long defValue) {
        return prefs.getLong(key, defValue);
    }

    private void putFloat(String key, float value) {
        prefs.edit().putFloat(key, value).apply();
    }

    private float getFloat(String key, float defValue) {
        return prefs.getFloat(key, defValue);
    }

    private void putBoolean(String key, boolean value) {
        prefs.edit().putBoolean(key, value).apply();
    }

    private boolean getBoolean(String key, boolean defValue) {
        return prefs.getBoolean(key, defValue);
    }

    /**
     * Save an object to preferences by serializing it to JSON.
     *
     * @param key    The preference key.
     * @param object The object to save (must be serializable by Gson).
     * @param <T>    The type of the object.
     */
    public <T> void putObject(String key, T object) {
        String json = gson.toJson(object);
        putString(key, json);
    }

    /**
     * Retrieve an object from preferences by deserializing it from JSON.
     *
     * @param key         The preference key.
     * @param objectClass The class of the object to retrieve.
     * @param <T>         The type of the object.
     * @return The deserialized object, or null if not found or an error occurs.
     */
    @Nullable
    public <T> T getObject(String key, Class<T> objectClass) {
        String json = getString(key, null);
        if (json == null) {
            return null;
        }
        try {
            return gson.fromJson(json, objectClass);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Clear all preferences.
     */
    public void clear() {
        prefs.edit().clear().apply();
    }

    /**
     * Remove a specific preference by key.
     *
     * @param key The key of the preference to remove.
     */
    public void remove(String key) {
        prefs.edit().remove(key).apply();
    }

    /**
     * Check if a preference exists.
     *
     * @param key The key to check.
     * @return true if the preference exists, false otherwise.
     */
    public boolean contains(String key) {
        return prefs.contains(key);
    }
}
