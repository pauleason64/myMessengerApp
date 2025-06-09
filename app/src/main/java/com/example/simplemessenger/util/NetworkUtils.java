package com.example.SImpleMessenger.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

import com.example.SImpleMessenger.R;

/**
 * Utility class for network-related operations.
 */
public class NetworkUtils {

    /**
     * Checks if the device has an active internet connection.
     *
     * @param context The application context.
     * @return true if there is an active internet connection, false otherwise.
     */
    public static boolean isNetworkAvailable(Context context) {
        if (context == null) return false;

        ConnectivityManager connectivityManager = 
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) return false;
            
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null && 
                   (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
    }

    /**
     * Checks if the device is connected to a metered network (like mobile data).
     *
     * @param context The application context.
     * @return true if connected to a metered network, false otherwise.
     */
    public static boolean isMeteredNetwork(Context context) {
        if (context == null) return false;

        ConnectivityManager connectivityManager = 
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) return false;
            
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null && 
                   (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    connectivityManager.isActiveNetworkMetered());
        } else {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && 
                   activeNetwork.isConnected() &&
                   (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE ||
                    connectivityManager.isActiveNetworkMetered());
        }
    }

    /**
     * Gets the current network type as a string.
     *
     * @param context The application context.
     * @return A string representing the current network type.
     */
    public static String getNetworkType(Context context) {
        if (context == null) return context.getString(R.string.unknown);

        ConnectivityManager connectivityManager = 
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager == null) {
            return context.getString(R.string.disconnected);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) return context.getString(R.string.disconnected);
            
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            if (capabilities == null) return context.getString(R.string.unknown);
            
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return context.getString(R.string.wifi);
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return context.getString(R.string.mobile_data);
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                return context.getString(R.string.ethernet);
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                return context.getString(R.string.vpn);
            } else {
                return context.getString(R.string.unknown);
            }
        } else {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            if (activeNetwork == null || !activeNetwork.isConnected()) {
                return context.getString(R.string.disconnected);
            }
            
            int type = activeNetwork.getType();
            switch (type) {
                case ConnectivityManager.TYPE_WIFI:
                    return context.getString(R.string.wifi);
                case ConnectivityManager.TYPE_MOBILE:
                    return context.getString(R.string.mobile_data);
                case ConnectivityManager.TYPE_ETHERNET:
                    return context.getString(R.string.ethernet);
                case ConnectivityManager.TYPE_VPN:
                    return context.getString(R.string.vpn);
                default:
                    return context.getString(R.string.unknown);
            }
        }
    }
}
