package com.example.simplemessenger.util;

import android.content.Context;
import android.text.format.DateUtils;

import com.example.simplemessenger.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DateTimeUtils {

    public static String formatMessageTime(Context context, long timestamp) {
        if (timestamp <= 0) {
            return "";
        }

        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        
        // If within the same day
        if (DateUtils.isToday(timestamp)) {
            return formatTime(timestamp);
        } 
        // If yesterday
        else if (DateUtils.isToday(timestamp + TimeUnit.DAYS.toMillis(1))) {
            return context.getString(R.string.yesterday);
        }
        // If within the last week
        else if (diff < TimeUnit.DAYS.toMillis(7)) {
            return new SimpleDateFormat("EEEE", Locale.getDefault()).format(new Date(timestamp));
        }
        // Otherwise, show the date
        else {
            return formatDate(timestamp);
        }
    }

    public static String formatMessageDateTime(Context context, long timestamp) {
        if (timestamp <= 0) {
            return "";
        }

        if (DateUtils.isToday(timestamp)) {
            return context.getString(R.string.today) + " " + formatTime(timestamp);
        } else if (DateUtils.isToday(timestamp + TimeUnit.DAYS.toMillis(1))) {
            return context.getString(R.string.yesterday) + " " + formatTime(timestamp);
        } else {
            return formatDateTime(timestamp);
        }
    }

    public static String formatTime(long timestamp) {
        return new SimpleDateFormat("h:mm a", Locale.getDefault())
                .format(new Date(timestamp));
    }

    public static String formatDate(long timestamp) {
        return new SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                .format(new Date(timestamp));
    }

    public static String formatDateTime(long timestamp) {
        return new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
                .format(new Date(timestamp));
    }

    public static String formatFullDate(long timestamp) {
        return new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
            .format(new Date(timestamp));
    }

    public static String getRelativeTimeSpanString(Context context, long timestamp) {
        if (timestamp <= 0) {
            return "";
        }

        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < TimeUnit.MINUTES.toMillis(1)) {
            return context.getString(R.string.just_now);
        } else if (diff < TimeUnit.HOURS.toMillis(1)) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
            return context.getResources().getQuantityString(
                    R.plurals.minutes_ago, (int) minutes, (int) minutes);
        } else if (diff < TimeUnit.DAYS.toMillis(1)) {
            long hours = TimeUnit.MILLISECONDS.toHours(diff);
            return context.getResources().getQuantityString(
                    R.plurals.hours_ago, (int) hours, (int) hours);
        } else if (diff < TimeUnit.DAYS.toMillis(7)) {
            long days = TimeUnit.MILLISECONDS.toDays(diff);
            return context.getResources().getQuantityString(
                    R.plurals.days_ago, (int) days, (int) days);
        } else if (diff < TimeUnit.DAYS.toMillis(30)) {
            long weeks = TimeUnit.MILLISECONDS.toDays(diff) / 7;
            return context.getResources().getQuantityString(
                    R.plurals.weeks_ago, (int) weeks, (int) weeks);
        } else if (diff < TimeUnit.DAYS.toMillis(365)) {
            long months = TimeUnit.MILLISECONDS.toDays(diff) / 30;
            return context.getResources().getQuantityString(
                    R.plurals.months_ago, (int) months, (int) months);
        } else {
            long years = TimeUnit.MILLISECONDS.toDays(diff) / 365;
            return context.getResources().getQuantityString(
                    R.plurals.years_ago, (int) years, (int) years);
        }
    }
}
