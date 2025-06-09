package com.example.SImpleMessenger.data.model;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;

@com.google.firebase.database.IgnoreExtraProperties

public class Message {
    private String id = "";
    private String senderId = "";
    private String senderEmail = "";
    private String recipientId = "";
    private String recipientEmail = "";
    private String subject = "";
    private String content = "";
    private long timestamp = 0;
    private boolean read = false;
    private boolean hasReminder = false;
    private long reminderTime = 0;
    private boolean archived = false;
    @com.google.firebase.database.PropertyName("isNote")
    private boolean isNote = false;
    private String previousMessageId = "";

    // Required empty constructor for Firebase
    public Message() {
        this.timestamp = 0; // Will be set when saving to database
    }

    public Message(String senderId, String senderEmail, String recipientId, String recipientEmail, 
                  String subject, String content) {
        this(senderId, senderEmail, recipientId, recipientEmail, subject, content, false);
    }

    public Message(String senderId, String senderEmail, String recipientId, String recipientEmail, 
                  String subject, String content, boolean isNote) {
        this(senderId, senderEmail, recipientId, recipientEmail, subject, content, isNote, "");
    }
    
    public Message(String senderId, String senderEmail, String recipientId, String recipientEmail, 
                  String subject, String content, boolean isNote, String previousMessageId) {
        this.senderId = senderId;
        this.senderEmail = senderEmail;
        this.recipientId = recipientId;
        this.recipientEmail = recipientEmail;
        this.subject = subject;
        this.content = content;
        this.read = false;
        this.hasReminder = false;
        this.archived = false;
        this.isNote = isNote;
        this.previousMessageId = previousMessageId != null ? previousMessageId : "";
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

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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
    @Exclude
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
    
    // This annotation tells Firebase to use the Object version of setReminderTime
    @com.google.firebase.database.Exclude
    public void setReminderTime(long reminderTime) {
        this.reminderTime = reminderTime;
        this.hasReminder = true;
    }
    
    /**
     * Formats the timestamp for display in message headers
     * @return Formatted date/time string
     */
    @Exclude
    public String getFormattedTimestamp() {
        try {
            return android.text.format.DateFormat.format("MMM d, yyyy h:mm a", new java.util.Date(timestamp)).toString();
        } catch (Exception e) {
            return "";
        }
    }
    
    // Standard getter/setter with Firebase annotations
    @com.google.firebase.database.Exclude
    public boolean isNote() {
        return isNote;
    }

    @com.google.firebase.database.Exclude
    public void setNote(boolean isNote) {
        this.isNote = isNote;
    }

    // These methods are used by Firebase for serialization/deserialization
    @com.google.firebase.database.PropertyName("isNote")
    public boolean getIsNote() {
        return isNote;
    }

    @com.google.firebase.database.PropertyName("isNote")
    public void setIsNote(boolean isNote) {
        this.isNote = isNote;
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
        map.put("recipientId", recipientId);  // Added missing recipientId
        map.put("recipientEmail", recipientEmail);
        map.put("subject", subject);
        map.put("content", content);
        map.put("timestamp", timestamp > 0 ? timestamp : ServerValue.TIMESTAMP);
        map.put("read", read);
        map.put("hasReminder", hasReminder);
        if (hasReminder) {
            map.put("reminderTime", reminderTime > 0 ? reminderTime : ServerValue.TIMESTAMP);
        }
        map.put("archived", archived);
        map.put("isNote", isNote);  // Uncommented and fixed to use the field directly
        if (previousMessageId != null && !previousMessageId.isEmpty()) {
            map.put("previousMessageId", previousMessageId);
        }
        return map;
    }

    public String getPreviousMessageId() {
        return previousMessageId;
    }

    public void setPreviousMessageId(String previousMessageId) {
        this.previousMessageId = previousMessageId != null ? previousMessageId : "";
    }
    
    public void setContents(String messageText) {
    }
}
