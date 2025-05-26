package com.example.simplemessenger.data.model;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties

public class Message {
    private String id;
    private String senderId;
    private String senderEmail;
    private String recipientEmail;
    private String subject;
    private String message;
    private long timestamp;
    private boolean read;
    private boolean hasReminder;
    private long reminderTime;
    private boolean archived;

    // Required empty constructor for Firebase
    public Message() {
        this.timestamp = 0; // Will be set when saving to database
    }

    public Message(String senderId, String senderEmail, String recipientEmail, 
                  String subject, String message) {
        this.senderId = senderId;
        this.senderEmail = senderEmail;
        this.recipientEmail = recipientEmail;
        this.subject = subject;
        this.message = message;
        this.read = false;
        this.hasReminder = false;
        this.archived = false;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Object timestamp) {
        // Handle both direct long values and ServerValue.TIMESTAMP
        if (timestamp instanceof Long) {
            this.timestamp = (Long) timestamp;
        } else if (timestamp instanceof Map) {
            // This handles ServerValue.TIMESTAMP which comes as a Map
            this.timestamp = System.currentTimeMillis();
        } else {
            this.timestamp = 0;
        }
    }
    
    // Overloaded setter for direct long assignment
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public boolean isHasReminder() {
        return hasReminder;
    }

    public void setHasReminder(boolean hasReminder) {
        this.hasReminder = hasReminder;
    }

    public long getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(Object reminderTime) {
        // Handle both direct long values and ServerValue.TIMESTAMP
        if (reminderTime instanceof Long) {
            this.reminderTime = (Long) reminderTime;
            this.hasReminder = true;
        } else if (reminderTime instanceof Map) {
            // This handles ServerValue.TIMESTAMP which comes as a Map
            this.reminderTime = System.currentTimeMillis();
            this.hasReminder = true;
        } else {
            this.reminderTime = 0;
            this.hasReminder = false;
        }
    }
    
    // Overloaded setter for direct long assignment
    public void setReminderTime(long reminderTime) {
        this.reminderTime = reminderTime;
        this.hasReminder = true;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    // Convert to Map for Realtime Database
    @Exclude
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("senderId", senderId);
        map.put("senderEmail", senderEmail);
        map.put("recipientEmail", recipientEmail);
        map.put("subject", subject);
        map.put("message", message);
        map.put("timestamp", timestamp > 0 ? timestamp : ServerValue.TIMESTAMP);
        map.put("read", read);
        map.put("hasReminder", hasReminder);
        if (hasReminder) {
            map.put("reminderTime", reminderTime > 0 ? reminderTime : ServerValue.TIMESTAMP);
        }
        map.put("archived", archived);
        return map;
    }
    
//    @Exclude
//    public long getReminderTimeLong() {
//        if (reminderTime instanceof Long) {
//            return (Long) reminderTime;
//        } else if (reminderTime instanceof Map) {
//            return (Long) ((Map<?, ?>) reminderTime).get("timestamp");
//        } else {
//            return 0;
//        }
//    }
}
