package com.example.simplemessenger;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Configuration;
import androidx.work.WorkManager;

import com.example.simplemessenger.util.FirebaseFactory;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthSettings;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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
            Log.d("SimpleMessengerApp", "Starting Firebase initialization...");
            
            // 1. First ensure FirebaseApp is initialized
            FirebaseApp firebaseApp = null;
            try {
                firebaseApp = FirebaseApp.initializeApp(this);
                Log.d("SimpleMessengerApp", "FirebaseApp initialized successfully");
            } catch (IllegalStateException e) {
                // If already initialized, get the default app
                firebaseApp = FirebaseApp.getInstance();
                Log.d("SimpleMessengerApp", "FirebaseApp already initialized");
            }
            
            if (firebaseApp == null) {
                Log.e("SimpleMessengerApp", "Failed to initialize FirebaseApp");
                return;
            }
            
            // 2. Initialize Firebase components with proper error handling
            try {
                // Initialize our custom Firebase configuration first
                FirebaseFactory.initialize(this);
                
                // Set up database persistence
                FirebaseDatabase database = FirebaseFactory.getDatabase();
                database.setPersistenceEnabled(true);
                Log.d("SimpleMessengerApp", "Firebase Database persistence enabled");
                
                // Configure Firebase Auth after database is set up
                try {
                    FirebaseAuth auth = FirebaseAuth.getInstance();
                    FirebaseAuthSettings firebaseAuthSettings = auth.getFirebaseAuthSettings();
                    
                    // Disable app verification for testing (only for development!)
                    firebaseAuthSettings.setAppVerificationDisabledForTesting(true);
                    
                    Log.d("SimpleMessengerApp", "Firebase Auth configured with app verification disabled");
                } catch (Exception e) {
                    Log.e("SimpleMessengerApp", "Error configuring Firebase Auth", e);
                }
                
                // Verify database connection
                database.getReference(".info/connected").addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Boolean connected = snapshot.getValue(Boolean.class);
                        if (connected != null && connected) {
                            Log.d("SimpleMessengerApp", "Connected to Firebase Database");
                        } else {
                            Log.d("SimpleMessengerApp", "Not connected to Firebase Database");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("SimpleMessengerApp", "Firebase Database connection error", error.toException());
                    }
                });
                
            } catch (Exception e) {
                Log.e("SimpleMessengerApp", "Error initializing Firebase components", e);
                // Try to recover by initializing with default options
                try {
                    FirebaseApp.initializeApp(this, FirebaseOptions.fromResource(this));
                    Log.e("SimpleMessengerApp", "Recovered Firebase initialization with default options");
                } catch (Exception ex) {
                    Log.e("SimpleMessengerApp", "Failed to recover Firebase initialization", ex);
                }
            }
        } catch (Exception e) {
            Log.e("SimpleMessengerApp", "Unexpected error during Firebase initialization", e);
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
