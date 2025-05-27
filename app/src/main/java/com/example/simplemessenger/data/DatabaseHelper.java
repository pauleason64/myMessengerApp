package com.example.simplemessenger.data;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.simplemessenger.data.model.Message;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class DatabaseHelper {
    private static final String TAG = "DatabaseHelsetPersistenceEnabledper";
    private static DatabaseHelper instance;
    private final DatabaseReference databaseReference;
    private final FirebaseAuth mAuth;

    // Database paths
    private static final String MESSAGES_NODE = "messages";
    private static final String USER_MESSAGES_NODE = "user-messages";
    private static final String USER_SENT_NODE = "sent";
    private static final String USER_RECEIVED_NODE = "received";

    private DatabaseHelper() {
        // Initialize Firebase Database with custom URL
        databaseReference = FirebaseDatabase.getInstance("https://simplemessenger-c0a47-default-rtdb.europe-west1.firebasedatabase.app").getReference();
        mAuth = FirebaseAuth.getInstance();
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

        message.setId(messageId);
        // The timestamp will be set by the Message.toMap() method using ServerValue.TIMESTAMP
        
        // Create a map to update multiple locations at once
        String currentUserId = mAuth.getCurrentUser().getUid();
        String recipientId = getUserIdFromEmail(message.getRecipientEmail());
        
        Map<String, Object> updates = new HashMap<>();
        
        // Add the message to the messages node
        updates.put("/" + MESSAGES_NODE + "/" + messageId, message.toMap());
        
        // Add reference to sender's sent messages
        updates.put("/" + USER_MESSAGES_NODE + "/" + currentUserId + "/" + USER_SENT_NODE + "/" + messageId, true);
        
        // Add reference to recipient's received messages
        updates.put("/" + USER_MESSAGES_NODE + "/" + recipientId + "/" + USER_RECEIVED_NODE + "/" + messageId, true);
        
        // Perform all updates as a single atomic operation
        databaseReference.updateChildren(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess(messageId))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error sending message: " + e.getMessage());
                    callback.onError(e.getMessage());
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

    // Callback interface for database operations
    public interface DatabaseCallback {
        void onSuccess(Object result);
        void onError(String error);
    }
    
    /**
     * Get the database reference for direct database operations.
     * @return The DatabaseReference instance
     */
    public DatabaseReference getDatabaseReference() {
        return databaseReference;
    }
}
