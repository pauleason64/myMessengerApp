package com.example.simplemessenger;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.work.Configuration;
import androidx.work.WorkManager;

import com.example.simplemessenger.util.FirebaseFactory;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthSettings;

public class SimpleMessengerApp extends Application implements Configuration.Provider {
    
    public static final String CHANNEL_ID = "simple_messenger_channel";
    public static final String MESSAGES_COLLECTION = "messages";
    public static final String USERS_COLLECTION = "users";
    public static final String SHARED_PREFS_NAME = "simple_messenger_prefs";
    public static final String PREF_USER_LOGGED_IN = "user_logged_in";
    public static final String PREF_USER_ID = "user_id";
    public static final String PREF_USER_EMAIL = "user_email";
    public static final String PREF_USER_NAME = "user_name";
    public static final String PREF_REMEMBER_ME = "remember_me";

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize Firebase
        try {
            // First initialize FirebaseApp
            FirebaseApp.initializeApp(this);
            
            // Configure Firebase Auth to disable app verification for testing
            try {
                FirebaseAuth auth = FirebaseAuth.getInstance();
                FirebaseAuthSettings firebaseAuthSettings = auth.getFirebaseAuthSettings();
                
                // Disable app verification for testing (only for development!)
                firebaseAuthSettings.setAppVerificationDisabledForTesting(true);
                
                Log.d("SimpleMessengerApp", "Firebase Auth configured with app verification disabled");
            } catch (Exception e) {
                Log.e("SimpleMessengerApp", "Error configuring Firebase Auth", e);
            }
            
            // Initialize our custom Firebase configuration
            FirebaseFactory.initialize(this);
            FirebaseFactory.getDatabase().setPersistenceEnabled(true);
            Log.d("SimpleMessengerApp", "Firebase Database persistence enabled");
        } catch (Exception e) {
            Log.e("SimpleMessengerApp", "Failed to initialize Firebase", e);
        }
        
        // Create notification channel
        createNotificationChannel();
        
        // Initialize WorkManager
        WorkManager.initialize(this, getWorkManagerConfiguration());
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
    
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.INFO)
                .build();
    }
}
