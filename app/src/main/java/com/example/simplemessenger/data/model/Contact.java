package com.example.simplemessenger.data.model;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class Contact {
    private String id;
    private String userId;  // ID of the current user who owns this contact
    private String contactId;  // ID of the contact user
    private String userName;
    private String displayName;  // Required by database rules
    private String emailAddress;
    private long timestamp;
    private boolean customName = false; // Whether the display name was set by the user

    // Required empty constructor for Firebase
    public Contact() {
    }

    public Contact(String userId, String contactId, String userName, String emailAddress) {
        this.userId = userId;
        this.contactId = contactId;
        this.userName = userName;
        this.displayName = userName;  // Set displayName to userName by default
        this.emailAddress = emailAddress;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    @Exclude
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }


    public String getContactId() {
        return contactId;
    }

    public void setContactId(String contactId) {
        this.contactId = contactId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getDisplayName() {
        return displayName != null ? displayName : userName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        // Also update userName if it's null to maintain consistency
        if (this.userName == null) {
            this.userName = displayName;
        }
    }
    
    public boolean isCustomName() {
        return customName;
    }
    
    public void setCustomName(boolean customName) {
        this.customName = customName;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("contactId", contactId);
        result.put("userName", userName);
        result.put("displayName", getDisplayName());  // Always include displayName
        result.put("customName", customName);
        result.put("emailAddress", emailAddress);
        result.put("timestamp", timestamp);
        return result;
    }
}
