package com.example.simplemessenger.util;

import android.util.Log;

public class LogWrapper {
    public void d(String tag, String message) {
        Log.d(tag, message);
    }
    
    public void e(String tag, String message, Throwable e) {
        Log.e(tag, message, e);
    }
}
