package com.example.simplemessenger.util;

import android.util.Log;

/**
 * A wrapper around Android's Log class to make it more testable.
 */
public class LogWrapper {
    
    /**
     * Send a DEBUG log message.
     *
     * @param tag Used to identify the source of a log message.
     * @param message The message you would like logged.
     */
    public void d(String tag, String message) {
        Log.d(tag, message);
    }
    
    /**
     * Send an ERROR log message.
     *
     * @param tag Used to identify the source of a log message.
     * @param message The message you would like logged.
     * @param throwable An exception to log.
     */
    public void e(String tag, String message, Throwable throwable) {
        Log.e(tag, message, throwable);
    }
}
