package com.example.simplemessenger.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.simplemessenger.SimpleMessengerApp;

public class AuthUtils {
    private static final String TAG = "AuthUtils";

    /**
     * Checks if a user is currently logged in
     * @param context The application context
     * @return true if user is logged in, false otherwise
     */
    public static boolean isUserLoggedIn(Context context) {
        try {
            SharedPreferences sharedPreferences = context.getSharedPreferences(
                    SimpleMessengerApp.SHARED_PREFS_NAME,
                    Context.MODE_PRIVATE
            );
            
            boolean isLoggedIn = sharedPreferences.getBoolean(
                    SimpleMessengerApp.PREF_USER_LOGGED_IN, 
                    false
            );
            
            Log.d(TAG, "isUserLoggedIn: " + isLoggedIn);
            return isLoggedIn;
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking login status", e);
            return false;
        }
    }

    /**
     * Saves the login state to SharedPreferences
     * @param context The application context
     * @param isLoggedIn Whether the user is logged in
     * @param email User's email (can be null)
     * @param userId User's ID (can be null)
     */
    public static void saveLoginState(Context context, boolean isLoggedIn, String email, String userId) {
        try {
            SharedPreferences.Editor editor = context.getSharedPreferences(
                    SimpleMessengerApp.SHARED_PREFS_NAME,
                    Context.MODE_PRIVATE
            ).edit();
            
            editor.putBoolean(SimpleMessengerApp.PREF_USER_LOGGED_IN, isLoggedIn);
            
            if (email != null) {
                editor.putString(SimpleMessengerApp.PREF_USER_EMAIL, email);
            }
            
            if (userId != null) {
                editor.putString(SimpleMessengerApp.PREF_USER_ID, userId);
            }
            
            editor.apply();
            Log.d(TAG, "Login state saved: " + isLoggedIn);
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving login state", e);
        }
    }

    /**
     * Clears the login state from SharedPreferences
     * @param context The application context
     */
    public static void clearLoginState(Context context) {
        try {
            SharedPreferences.Editor editor = context.getSharedPreferences(
                    SimpleMessengerApp.SHARED_PREFS_NAME,
                    Context.MODE_PRIVATE
            ).edit();
            
            editor.remove(SimpleMessengerApp.PREF_USER_LOGGED_IN);
            editor.remove(SimpleMessengerApp.PREF_USER_EMAIL);
            editor.remove(SimpleMessengerApp.PREF_USER_ID);
            editor.apply();
            
            Log.d(TAG, "Login state cleared");
            
        } catch (Exception e) {
            Log.e(TAG, "Error clearing login state", e);
        }
    }

    /**
     * Checks authentication state and redirects to the appropriate activity
     * @param context The context to use for starting activities
     * @param authActivityClass The AuthActivity class to redirect to if not authenticated
     * @param mainActivityClass The MainActivity class to redirect to if authenticated
     * @return true if the user is authenticated, false otherwise
     */
    public static boolean checkAuthState(Context context, 
                                       Class<?> authActivityClass, 
                                       Class<?> mainActivityClass) {
        try {
            // Don't redirect if we're already in the correct activity
            boolean isInAuthActivity = context.getClass().equals(authActivityClass);
            boolean isInMainActivity = context.getClass().equals(mainActivityClass);
            
            if (isUserLoggedIn(context)) {
                // User is logged in
                if (!isInMainActivity) {
                    Log.d(TAG, "User is authenticated, redirecting to main activity");
                    Intent intent = new Intent(context, mainActivityClass);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | 
                                  Intent.FLAG_ACTIVITY_NEW_TASK | 
                                  Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    context.startActivity(intent);
                    if (context instanceof Activity) {
                        ((Activity) context).finish();
                    }
                }
                return true;
            } else {
                // User is not logged in
                if (!isInAuthActivity) {
                    Log.d(TAG, "User is not authenticated, redirecting to auth activity");
                    Intent intent = new Intent(context, authActivityClass);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | 
                                  Intent.FLAG_ACTIVITY_NEW_TASK | 
                                  Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    context.startActivity(intent);
                    if (context instanceof Activity) {
                        ((Activity) context).finish();
                    }
                }
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking auth state", e);
            return false;
        }
    }
}
