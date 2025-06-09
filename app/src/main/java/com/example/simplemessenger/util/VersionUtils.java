package com.example.SImpleMessenger.util;

import android.os.Build;

/**
 * Utility class for handling version-specific functionality.
 */
public class VersionUtils {
    
    private VersionUtils() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Check if the device is running Android 6.0 (Marshmallow) or higher.
     *
     * @return true if the device is running Android 6.0 or higher, false otherwise.
     */
    public static boolean hasMarshmallow() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }
    
    /**
     * Check if the device is running Android 7.0 (Nougat) or higher.
     *
     * @return true if the device is running Android 7.0 or higher, false otherwise.
     */
    public static boolean hasNougat() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
    }
    
    /**
     * Check if the device is running Android 8.0 (Oreo) or higher.
     *
     * @return true if the device is running Android 8.0 or higher, false otherwise.
     */
    public static boolean hasOreo() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }
    
    /**
     * Check if the device is running Android 10.0 (Q) or higher.
     *
     * @return true if the device is running Android 10.0 or higher, false otherwise.
     */
    public static boolean hasQ() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    }
    
    /**
     * Check if the device is running Android 11.0 (R) or higher.
     *
     * @return true if the device is running Android 11.0 or higher, false otherwise.
     */
    public static boolean hasR() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R;
    }
    
    /**
     * Check if the device is running Android 12.0 (S) or higher.
     *
     * @return true if the device is running Android 12.0 or higher, false otherwise.
     */
    public static boolean hasS() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
    }
    
    /**
     * Check if the device is running Android 13.0 (TIRAMISU) or higher.
     *
     * @return true if the device is running Android 13.0 or higher, false otherwise.
     */
    public static boolean hasTiramisu() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;
    }
    
    /**
     * Check if the device is running Android 14.0 (UPSIDE_DOWN_CAKE) or higher.
     *
     * @return true if the device is running Android 14.0 or higher, false otherwise.
     */
    public static boolean hasUpsideDownCake() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE;
    }
    
    /**
     * Get the current Android version code name.
     *
     * @return The version code name as a String.
     */
    public static String getAndroidVersionName() {
        return Build.VERSION.RELEASE;
    }
    
    /**
     * Get the current Android SDK version.
     *
     * @return The SDK version as an integer.
     */
    public static int getAndroidSdkVersion() {
        return Build.VERSION.SDK_INT;
    }
    
    /**
     * Check if the device is running a version of Android that supports scoped storage.
     * Scoped storage was introduced in Android 10 (API 29) and made mandatory in Android 11 (API 30).
     *
     * @return true if the device is running Android 10 or higher, false otherwise.
     */
    public static boolean hasScopedStorage() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    }
    
    /**
     * Check if the device is running a version of Android that requires background location permission.
     * Background location permission was introduced in Android 10 (API 29).
     *
     * @return true if the device is running Android 10 or higher, false otherwise.
     */
    public static boolean requiresBackgroundLocationPermission() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    }
    
    /**
     * Check if the device is running a version of Android that requires notification permission.
     * Notification permission was introduced in Android 13 (API 33).
     *
     * @return true if the device is running Android 13 or higher, false otherwise.
     */
    public static boolean requiresNotificationPermission() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;
    }
    
    /**
     * Check if the device is running a version of Android that requires foreground service type.
     * Foreground service types were introduced in Android 10 (API 29).
     *
     * @return true if the device is running Android 10 or higher, false otherwise.
     */
    public static boolean requiresForegroundServiceType() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    }
    
    /**
     * Get the device manufacturer and model information.
     *
     * @return A string containing the device manufacturer and model.
     */
    public static String getDeviceInfo() {
        return Build.MANUFACTURER + " " + Build.MODEL;
    }
}
