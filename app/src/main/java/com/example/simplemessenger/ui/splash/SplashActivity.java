package com.example.SImpleMessenger.ui.splash;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.SImpleMessenger.SimpleMessengerApp;
import com.example.SImpleMessenger.R;
import com.example.SImpleMessenger.ui.auth.AuthActivity;
import com.example.SImpleMessenger.ui.main.MainActivity;
import com.example.SImpleMessenger.utils.AuthUtils;

public class SplashActivity extends AppCompatActivity {
    private static final long SPLASH_DELAY = 1500; // 1.5 seconds
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(
                SimpleMessengerApp.SHARED_PREFS_NAME,
                Context.MODE_PRIVATE
        );

        // Delay for splash screen
        handler.postDelayed(this::checkAuthState, SPLASH_DELAY);
    }

    private void checkAuthState() {
        Log.d("SplashActivity", "Checking authentication state");
        // Use AuthUtils to handle the authentication check and redirection
        AuthUtils.checkAuthState(this, AuthActivity.class, MainActivity.class);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
