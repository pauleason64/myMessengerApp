package com.example.SImpleMessenger.data;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.SImpleMessenger.data.model.Message;
import com.example.SImpleMessenger.util.FirebaseFactory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseHelper {
    private static final String TAG = "DatabaseHelperPersistenceEnabled";
    private static DatabaseHelper instance;
    private final DatabaseReference databaseReference;
    private final FirebaseAuth mAuth;
    private boolean isNotes = false;
    private boolean isInbox = true;

    // Database paths
    protected static final String MESSAGES_NODE = "messages";
    protected static final String USER_MESSAGES_NODE = "user-messages";
    protected static final String USER_SENT_NODE = "sent";
    protected static final String USER_RECEIVED_NODE = "received";
    protected static final String USER_CATEGORY_NODE = "user-category";
    protected static final String CATEGORY_LIST_NODE = "categoryList";

    private DatabaseHelper() {
        this(FirebaseFactory.getDatabase().getReference(),
             FirebaseFactory.getAuth());
    }
    
    /**
     * Set the current tab state for message operations
     * @param isNotes Whether the current tab is the Notes tab
     * @param isInbox Whether the current tab is the Inbox tab (only relevant if not Notes)
     */
    public void setTabState(boolean isNotes, boolean isInbox) {
        this.isNotes = isNotes;
        this.isInbox = isInbox;
    }
    
    /**
     * Package-private constructor for testing
     */
    DatabaseHelper(String databaseUrl) {
        // Use FirebaseFactory to get the database reference
        this(
            FirebaseFactory.getDatabase().getReference(),
            FirebaseFactory.getAuth()
        );
    }
    
    /**
     * Package-private constructor for dependency injection in tests
     */
    DatabaseHelper(DatabaseReference databaseReference, FirebaseAuth firebaseAuth) {
        this.databaseReference = databaseReference;
        this.mAuth = firebaseAuth;
    }

    public static synchronized DatabaseHelper getInstance() {
        if (instance == null) {
            instance = new DatabaseHelper();
        }
        return instance;
    }

    // Send a new message
    public void sendMessage(Message message, final DatabaseCallback callback) {
        if (mAuth.getCurrentUser() == null) {
            callback.onError("User not authenticated");
            return;
        }

        String messageId = databaseReference.child(MESSAGES_NODE).push().getKey();
        if (messageId == null) {
            callback.onError("Failed to generate message ID");
            return;
        }

        // Get current user's email
        String currentEmail = mAuth.getCurrentUser().getEmail();
        if (currentEmail == null || currentEmail.isEmpty()) {
            callback.onError("User email not available");
            return;
        }

        // Set sender information
        message.setSenderId(mAuth.getCurrentUser().getUid());
        message.setSenderEmail(currentEmail);
        message.setId(messageId);
        
        // The timestamp will be set by the Message.toMap() method using ServerValue.TIMESTAMP
        
        // Create a map to update multiple locations at once
        String currentUserId = mAuth.getCurrentUser().getUid();
        Map<String, Object> updates = new HashMap<>();
        
        // Add the message to the messages node
        updates.put("/" + MESSAGES_NODE + "/" + messageId, message.toMap());
        
        if (message.isNote()) {
            // For notes, save to the user's notes node
            updates.put("/" + USER_MESSAGES_NODE + "/" + currentUserId + "/notes/" + messageId, true);
        } else {
            // For regular messages, validate recipient and set up sent/received references
            String recipientId = message.getRecipientId();
            
            // Validate recipient ID
            if (recipientId == null || recipientId.isEmpty()) {
                callback.onError("Recipient ID is not available. Please ensure the contact is valid.");
                return;
            }
            
            // Validate recipient ID format (should be a Firebase UID, not an email)
            if (recipientId.contains("@") || recipientId.contains(".")) {
                callback.onError("Invalid recipient ID format. Expected UID but got: " + recipientId);
                return;
            }
            
            // Add reference to sender's sent messages
            updates.put("/" + USER_MESSAGES_NODE + "/" + currentUserId + "/" + USER_SENT_NODE + "/" + messageId, true);
            
            // Add reference to recipient's received messages using their UID
            updates.put("/" + USER_MESSAGES_NODE + "/" + recipientId + "/" + USER_RECEIVED_NODE + "/" + messageId, true);
        }
        
        // Log the updates map for debugging
        try {
            com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
            String updatesJson = gson.toJson(updates);
            Log.d(TAG, "Attempting to update database with: \n" + updatesJson);
        } catch (Exception e) {
            Log.e(TAG, "Error logging updates: " + e.getMessage());
        }

        // Perform all updates as a single atomic operation
        databaseReference.updateChildren(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess(messageId))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error sending message: " + e.getMessage());
                    callback.onError("Firebase Database error: " + e.getMessage());
                });
    }

    // Get all messages for the current user
    public void getUserMessages(String userId, boolean isSent, final DatabaseCallback callback) {
        String messageType = isSent ? USER_SENT_NODE : USER_RECEIVED_NODE;
        
        databaseReference.child(USER_MESSAGES_NODE).child(userId).child(messageType)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        // Handle the list of message IDs
                        // You'll need to fetch each message from the messages node
                        // using the message IDs
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        callback.onError(databaseError.getMessage());
                    }
                });
    }

    // Mark a message as read
    public void markMessageAsRead(String messageId) {
        databaseReference.child(MESSAGES_NODE).child(messageId).child("read").setValue(true);
    }

    // Delete a message
    public void deleteMessage(String messageId, String currentUserId) {
        // Instead of deleting, we'll mark it as archived
        databaseReference.child(MESSAGES_NODE).child(messageId).child("archived").setValue(true);
        
        // Remove from user's message lists
        databaseReference.child(USER_MESSAGES_NODE).child(currentUserId)
                .child(USER_RECEIVED_NODE).child(messageId).removeValue();
        databaseReference.child(USER_MESSAGES_NODE).child(currentUserId)
                .child(USER_SENT_NODE).child(messageId).removeValue();
    }

    // Helper method to convert email to user ID (replace @ and . with _)
    private String getUserIdFromEmail(String email) {
        return email.replace("@", "_").replace(".", "_");
    }
    
    /**
     * Fetches a single message by its ID
     * @param messageId The ID of the message to fetch
     * @param callback Callback to handle the result or error
     */
    public void getMessage(String messageId, final DatabaseCallback callback) {
        if (messageId == null || messageId.isEmpty()) {
            callback.onError("Message ID cannot be null or empty");
            return;
        }
        
        databaseReference.child(MESSAGES_NODE).child(messageId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            Message message = dataSnapshot.getValue(Message.class);
                            if (message != null) {
                                message.setId(dataSnapshot.getKey());
                                callback.onSuccess(message);
                                return;
                            }
                        }
                        callback.onError("Message not found");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "Error getting message: " + databaseError.getMessage());
                        callback.onError(databaseError.getMessage());
                    }
                });
    }

    // Callback interface for database operations
    public interface DatabaseCallback {
        default void onSuccess() {
            onSuccess(null);
        }
        
        default void onSuccess(Object result) {
            // Default implementation for backward compatibility
        }

        void onError(String error);
    }
    
    // Callback interface for message operations
    public interface MessageOperationCallback {
        void onSuccess();
        void onError(String error);
    }
    
    /**
     * Save user categories to the database
     * @param userId The ID of the user
     * @param categories List of categories to save (as maps)
     * @param callback Callback for success/error
     */
    public void saveUserCategories(String userId, List<Map<String, Object>> categories, final DatabaseCallback callback) {
        if (userId == null || userId.isEmpty()) {
            if (callback != null) {
                callback.onError("Invalid user ID");
            }
            return;
        }
        
        // Create the data structure to save
        Map<String, Object> categoryData = new HashMap<>();
        categoryData.put("categories", categories);
        
        // Save to Firebase using the database reference from FirebaseFactory
        databaseReference.child(USER_CATEGORY_NODE)
                .child(userId)
                .child(CATEGORY_LIST_NODE)
                .setValue(categoryData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Categories saved for user: " + userId);
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving categories", e);
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                });
    }
    
    /**
     * Check if a user has categories saved
     * @param userId The ID of the user
     * @param callback Callback with success/error
     */
    public void checkUserCategoriesExist(String userId, final DatabaseCallback callback) {
        if (userId == null || userId.isEmpty()) {
            if (callback != null) {
                callback.onError("Invalid user ID");
            }
            return;
        }
        
        databaseReference.child(USER_CATEGORY_NODE)
                .child(userId)
                .child(CATEGORY_LIST_NODE)
                .child("categories")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        boolean exists = dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0;
                        if (exists) {
                            callback.onSuccess();
                        } else {
                            callback.onError("No categories found");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "Error checking categories", databaseError.toException());
                        if (callback != null) {
                            callback.onError(databaseError.getMessage());
                        }
                    }
                });
    }
    
    /**
     * Get the database reference for direct database operations.
     * @return The DatabaseReference instance
     */
    public DatabaseReference getDatabaseReference() {
        return databaseReference;
    }
    
    /**
     * Delete multiple messages by their IDs
     * @param messageIds List of message IDs to delete
     * @param callback Callback to handle success or error
     */
    public void deleteMessages(List<String> messageIds, final MessageOperationCallback callback) {
        if (mAuth.getCurrentUser() == null) {
            if (callback != null) {
                callback.onError("User not authenticated");
            }
            return;
        }
        
        String currentUserId = mAuth.getCurrentUser().getUid();
        DatabaseReference userMessagesRef = databaseReference.child(USER_MESSAGES_NODE).child(currentUserId);
        
        // Determine the message type based on the current tab (inbox/outbox/notes)
        String messageType = isNotes ? "notes" : (isInbox ? USER_RECEIVED_NODE : USER_SENT_NODE);
        
        // Create a map to hold all updates
        Map<String, Object> updates = new HashMap<>();
        
        // Add each message to be deleted to the updates map
        for (String messageId : messageIds) {
            updates.put("/" + MESSAGES_NODE + "/" + messageId, null);
            updates.put("/" + USER_MESSAGES_NODE + "/" + currentUserId + "/" + messageType + "/" + messageId, null);
        }
        
        // Perform the updates atomically
        databaseReference.updateChildren(updates)
            .addOnSuccessListener(aVoid -> {
                if (callback != null) {
                    callback.onSuccess();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error deleting messages: " + e.getMessage());
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            });
    }
}
