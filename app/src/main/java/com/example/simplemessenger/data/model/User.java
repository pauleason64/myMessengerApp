package com.example.simplemessenger.data.model;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class User {
    private String uid;
    private String email;
    private String displayName;
    private long lastSeen;
    private boolean isOnline;
    private long createdAt;

    // Required empty constructor for Firebase
    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
        this.lastSeen = 0;
        this.isOnline = false;
        this.createdAt = System.currentTimeMillis();
    }

    public User(String uid, String email, String displayName) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.isOnline = false;
        this.lastSeen = System.currentTimeMillis();
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("uid", uid);
        result.put("email", email);
        result.put("displayName", displayName);
        result.put("lastSeen", lastSeen);
        result.put("isOnline", isOnline);
        result.put("createdAt", createdAt);
        return result;
    }

    @Override
    public String toString() {
        return "User{" +
                "uid='" + uid + '\'' +
                ", email='" + email + '\'' +
                ", displayName='" + displayName + '\'' +
                ", lastSeen=" + lastSeen +
                ", isOnline=" + isOnline +
                ", createdAt=" + createdAt +
                '}';
    }
}
