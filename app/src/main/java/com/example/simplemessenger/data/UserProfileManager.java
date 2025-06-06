package com.example.simplemessenger.data;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.simplemessenger.data.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.example.simplemessenger.util.FirebaseFactory;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class UserProfileManager {
    private static final String TAG = "UserProfileManager";
    private static final String USERS_NODE = "users";
    private static UserProfileManager instance;
    private final DatabaseReference databaseReference;
    private final FirebaseAuth auth;

    public interface ProfileUpdateListener {
        void onSuccess();
        void onError(String error);
    }

    private UserProfileManager() {
        this.databaseReference = FirebaseFactory.getDatabase().getReference();
        this.auth = FirebaseAuth.getInstance();
    }

    public static synchronized UserProfileManager getInstance() {
        if (instance == null) {
            instance = new UserProfileManager();
        }
        return instance;
    }

    /**
     * Create or update user profile in the database
     * @param userId The user's ID
     * @param email The user's email
     * @param displayName The user's display name
     * @param listener Callback for operation completion
     */
    public void createOrUpdateProfile(String userId, String email, String displayName, ProfileUpdateListener listener) {
        if (userId == null || userId.isEmpty()) {
            if (listener != null) {
                listener.onError("Invalid user ID");
            }
            return;
        }

        // First, check if the user already exists
        databaseReference.child(USERS_NODE).child(userId).addListenerForSingleValueEvent(
            new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    User user;
                    if (dataSnapshot.exists()) {
                        // Update existing user
                        user = dataSnapshot.getValue(User.class);
                        if (user == null) {
                            user = new User();
                        }
                        user.setDisplayName(displayName);
                        if (email != null && !email.isEmpty()) {
                            user.setEmail(email);
                        }
                    } else {
                        // Create new user
                        user = new User(userId, email != null ? email : "", displayName);
                        user.setCreatedAt(System.currentTimeMillis());
                    }
                    
                    // Update last seen and online status
                    user.setLastSeen(System.currentTimeMillis());
                    user.setOnline(true);
                    
                    // Save the user
                    databaseReference.child(USERS_NODE).child(userId)
                            .setValue(user)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "User profile updated successfully");
                                if (listener != null) {
                                    listener.onSuccess();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to update user profile: " + e.getMessage());
                                if (listener != null) {
                                    listener.onError(e.getMessage());
                                }
                            });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "Error checking user existence: " + databaseError.getMessage());
                    if (listener != null) {
                        listener.onError(databaseError.getMessage());
                    }
                }
            }
        );
    }

    /**
     * Get the current user's profile
     * @param listener Callback with the user's profile data
     */
    public void getCurrentUserProfile(ProfileLoadListener listener) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            if (listener != null) {
                listener.onError("User not authenticated");
            }
            return;
        }

        databaseReference.child(USERS_NODE).child(currentUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            // User exists, map to User object
                            User user = dataSnapshot.getValue(User.class);
                            if (user != null) {
                                user.setUid(currentUser.getUid());
                                // Ensure email is set (in case it was updated in Firebase Auth)
                                if (user.getEmail() == null || user.getEmail().isEmpty()) {
                                    user.setEmail(currentUser.getEmail());
                                }
                                
                                // Update last seen timestamp
                                user.setLastSeen(System.currentTimeMillis());
                                user.setOnline(true);
                                
                                // Save the updated timestamp
                                databaseReference.child(USERS_NODE).child(currentUser.getUid())
                                        .child("lastSeen").setValue(user.getLastSeen());
                                        
                                databaseReference.child(USERS_NODE).child(currentUser.getUid())
                                        .child("isOnline").setValue(true);
                                
                                if (listener != null) {
                                    listener.onProfileLoaded(user);
                                }
                            } else {
                                handleMissingProfile(currentUser, listener);
                            }
                        } else {
                            // Create a basic profile if it doesn't exist
                            handleMissingProfile(currentUser, listener);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "Error loading user profile: " + databaseError.getMessage());
                        if (listener != null) {
                            listener.onError(databaseError.getMessage());
                        }
                    }
                });
    }
    
    private void handleMissingProfile(FirebaseUser firebaseUser, ProfileLoadListener listener) {
        // Create a new user with basic info
        String displayName = firebaseUser.getDisplayName();
        if (displayName == null || displayName.isEmpty()) {
            // Try to get name from email
            String email = firebaseUser.getEmail();
            if (email != null && email.contains("@")) {
                displayName = email.substring(0, email.indexOf('@'));
            } else {
                displayName = "User";
            }
        }
        
        User newUser = new User(
            firebaseUser.getUid(),
            firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "",
            displayName
        );
        newUser.setCreatedAt(System.currentTimeMillis());
        newUser.setLastSeen(System.currentTimeMillis());
        newUser.setOnline(true);
        
        // Save the new user
        databaseReference.child(USERS_NODE).child(firebaseUser.getUid())
                .setValue(newUser)
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) {
                        listener.onProfileLoaded(newUser);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create user profile: " + e.getMessage());
                    if (listener != null) {
                        listener.onError("Failed to create user profile: " + e.getMessage());
                    }
                });
    }

    public interface ProfileLoadListener {
        /**
         * Called when the user profile is successfully loaded
         * @param user The loaded user profile
         */
        void onProfileLoaded(User user);
        
        /**
         * Called when there was an error loading the profile
         * @param error Error message describing what went wrong
         */
        void onError(String error);
    }
}
