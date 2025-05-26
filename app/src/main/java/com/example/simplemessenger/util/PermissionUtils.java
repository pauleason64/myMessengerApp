package com.example.simplemessenger.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for handling runtime permissions in Android 6.0 (API 23) and above.
 */
public class PermissionUtils {

    // Permission request codes
    public static final int PERMISSION_REQUEST_CODE = 100;
    public static final int PERMISSION_REQUEST_STORAGE = 101;
    public static final int PERMISSION_REQUEST_CAMERA = 102;
    public static final int PERMISSION_REQUEST_LOCATION = 103;
    public static final int PERMISSION_REQUEST_CONTACTS = 104;
    public static final int PERMISSION_REQUEST_PHONE = 105;
    public static final int PERMISSION_REQUEST_CALENDAR = 106;
    public static final int PERMISSION_REQUEST_MICROPHONE = 107;
    public static final int PERMISSION_REQUEST_SMS = 108;
    public static final int PERMISSION_REQUEST_NOTIFICATION = 109;

    // Permission groups
    public static final String[] STORAGE_PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public static final String[] CAMERA_PERMISSIONS = {
            Manifest.permission.CAMERA
    };

    public static final String[] LOCATION_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    public static final String[] CONTACTS_PERMISSIONS = {
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.GET_ACCOUNTS
    };

    public static final String[] PHONE_PERMISSIONS = {
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.WRITE_CALL_LOG,
            Manifest.permission.ADD_VOICEMAIL,
            Manifest.permission.USE_SIP,
            Manifest.permission.PROCESS_OUTGOING_CALLS
    };

    public static final String[] CALENDAR_PERMISSIONS = {
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
    };

    public static final String[] MICROPHONE_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO
    };

    public static final String[] SMS_PERMISSIONS = {
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_WAP_PUSH,
            Manifest.permission.RECEIVE_MMS
    };

    private PermissionUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Check if a permission is granted.
     *
     * @param context    The context.
     * @param permission The permission to check.
     * @return true if the permission is granted, false otherwise.
     */
    public static boolean hasPermission(@NonNull Context context, @NonNull String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Check if multiple permissions are granted.
     *
     * @param context     The context.
     * @param permissions The permissions to check.
     * @return true if all permissions are granted, false otherwise.
     */
    public static boolean hasPermissions(@NonNull Context context, @NonNull String... permissions) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true; // Permissions are granted at install time on older devices
        }

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Request permissions from an Activity.
     *
     * @param activity    The Activity.
     * @param permissions The permissions to request.
     * @param requestCode The request code.
     */
    public static void requestPermissions(@NonNull Activity activity, @NonNull String[] permissions, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(activity, permissions, requestCode);
        }
    }

    /**
     * Request permissions from a Fragment.
     *
     * @param fragment    The Fragment.
     * @param permissions The permissions to request.
     * @param requestCode The request code.
     */
    public static void requestPermissions(@NonNull Fragment fragment, @NonNull String[] permissions, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            fragment.requestPermissions(permissions, requestCode);
        }
    }

    /**
     * Check if the user has permanently denied any of the requested permissions.
     *
     * @param activity    The Activity.
     * @param permissions The permissions that were requested.
     * @return true if the user has permanently denied any of the permissions, false otherwise.
     */
    public static boolean shouldShowRequestPermissionRationale(@NonNull Activity activity, @NonNull String... permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the list of permissions that were denied from the last permission request.
     *
     * @param permissions  The requested permissions.
     * @param grantResults The grant results for the corresponding permissions.
     * @return An array of denied permissions.
     */
    @NonNull
    public static String[] getDeniedPermissions(@NonNull String[] permissions, @NonNull int[] grantResults) {
        List<String> deniedPermissions = new ArrayList<>();
        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                deniedPermissions.add(permissions[i]);
            }
        }
        return deniedPermissions.toArray(new String[0]);
    }

    /**
     * Check if the user has permanently denied any of the requested permissions.
     *
     * @param activity    The Activity.
     * @param permissions The permissions that were requested.
     * @return true if the user has permanently denied any of the permissions, false otherwise.
     */
    public static boolean hasPermanentlyDeniedPermissions(@NonNull Activity activity, @NonNull String... permissions) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false;
        }

        for (String permission : permissions) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, permission) &&
                    ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the user has granted all the requested permissions.
     *
     * @param grantResults The grant results for the corresponding permissions.
     * @return true if all permissions were granted, false otherwise.
     */
    public static boolean verifyPermissions(@NonNull int[] grantResults) {
        if (grantResults.length == 0) {
            return false;
        }

        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if the app has been granted the necessary storage permissions.
     *
     * @param context The context.
     * @return true if the app has storage permissions, false otherwise.
     */
    public static boolean hasStoragePermission(@NonNull Context context) {
        return hasPermissions(context, STORAGE_PERMISSIONS);
    }

    /**
     * Request storage permissions from an Activity.
     *
     * @param activity    The Activity.
     * @param requestCode The request code.
     */
    public static void requestStoragePermission(@NonNull Activity activity, int requestCode) {
        requestPermissions(activity, STORAGE_PERMISSIONS, requestCode);
    }

    /**
     * Check if the app has been granted the camera permission.
     *
     * @param context The context.
     * @return true if the app has camera permission, false otherwise.
     */
    public static boolean hasCameraPermission(@NonNull Context context) {
        return hasPermissions(context, CAMERA_PERMISSIONS);
    }

    /**
     * Request camera permission from an Activity.
     *
     * @param activity    The Activity.
     * @param requestCode The request code.
     */
    public static void requestCameraPermission(@NonNull Activity activity, int requestCode) {
        requestPermissions(activity, CAMERA_PERMISSIONS, requestCode);
    }

    /**
     * Check if the app has been granted the necessary location permissions.
     *
     * @param context The context.
     * @return true if the app has location permissions, false otherwise.
     */
    public static boolean hasLocationPermission(@NonNull Context context) {
        return hasPermissions(context, LOCATION_PERMISSIONS);
    }

    /**
     * Request location permissions from an Activity.
     *
     * @param activity    The Activity.
     * @param requestCode The request code.
     */
    public static void requestLocationPermission(@NonNull Activity activity, int requestCode) {
        requestPermissions(activity, LOCATION_PERMISSIONS, requestCode);
    }

    /**
     * Check if the app has been granted the necessary contacts permissions.
     *
     * @param context The context.
     * @return true if the app has contacts permissions, false otherwise.
     */
    public static boolean hasContactsPermission(@NonNull Context context) {
        return hasPermissions(context, CONTACTS_PERMISSIONS);
    }

    /**
     * Request contacts permissions from an Activity.
     *
     * @param activity    The Activity.
     * @param requestCode The request code.
     */
    public static void requestContactsPermission(@NonNull Activity activity, int requestCode) {
        requestPermissions(activity, CONTACTS_PERMISSIONS, requestCode);
    }

    /**
     * Check if the app has been granted the necessary phone permissions.
     *
     * @param context The context.
     * @return true if the app has phone permissions, false otherwise.
     */
    public static boolean hasPhonePermission(@NonNull Context context) {
        return hasPermissions(context, PHONE_PERMISSIONS);
    }

    /**
     * Request phone permissions from an Activity.
     *
     * @param activity    The Activity.
     * @param requestCode The request code.
     */
    public static void requestPhonePermission(@NonNull Activity activity, int requestCode) {
        requestPermissions(activity, PHONE_PERMISSIONS, requestCode);
    }

    /**
     * Check if the app has been granted the necessary calendar permissions.
     *
     * @param context The context.
     * @return true if the app has calendar permissions, false otherwise.
     */
    public static boolean hasCalendarPermission(@NonNull Context context) {
        return hasPermissions(context, CALENDAR_PERMISSIONS);
    }

    /**
     * Request calendar permissions from an Activity.
     *
     * @param activity    The Activity.
     * @param requestCode The request code.
     */
    public static void requestCalendarPermission(@NonNull Activity activity, int requestCode) {
        requestPermissions(activity, CALENDAR_PERMISSIONS, requestCode);
    }

    /**
     * Check if the app has been granted the microphone permission.
     *
     * @param context The context.
     * @return true if the app has microphone permission, false otherwise.
     */
    public static boolean hasMicrophonePermission(@NonNull Context context) {
        return hasPermissions(context, MICROPHONE_PERMISSIONS);
    }

    /**
     * Request microphone permission from an Activity.
     *
     * @param activity    The Activity.
     * @param requestCode The request code.
     */
    public static void requestMicrophonePermission(@NonNull Activity activity, int requestCode) {
        requestPermissions(activity, MICROPHONE_PERMISSIONS, requestCode);
    }

    /**
     * Check if the app has been granted the necessary SMS permissions.
     *
     * @param context The context.
     * @return true if the app has SMS permissions, false otherwise.
     */
    public static boolean hasSmsPermission(@NonNull Context context) {
        return hasPermissions(context, SMS_PERMISSIONS);
    }

    /**
     * Request SMS permissions from an Activity.
     *
     * @param activity    The Activity.
     * @param requestCode The request code.
     */
    public static void requestSmsPermission(@NonNull Activity activity, int requestCode) {
        requestPermissions(activity, SMS_PERMISSIONS, requestCode);
    }

    /**
     * Check if the app has been granted the notification permission (Android 13+).
     *
     * @param context The context.
     * @return true if the app has notification permission, false otherwise.
     */
    public static boolean hasNotificationPermission(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return hasPermission(context, Manifest.permission.POST_NOTIFICATIONS);
        }
        return true; // Permission not required before Android 13
    }

    /**
     * Request notification permission from an Activity (Android 13+).
     *
     * @param activity    The Activity.
     * @param requestCode The request code.
     */
    public static void requestNotificationPermission(@NonNull Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(activity, new String[]{Manifest.permission.POST_NOTIFICATIONS}, requestCode);
        }
    }
}
