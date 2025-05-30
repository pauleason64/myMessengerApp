package com.example.simplemessenger.data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.simplemessenger.data.model.Message;
import com.example.simplemessenger.util.FirebaseConfigManager;
import com.example.simplemessenger.util.FirebaseFactory;
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
    protected static final String MESSAGES_NODE = "messages";
    protected static final String USER_MESSAGES_NODE = "user-messages";
    protected static final String USER_SENT_NODE = "sent";
    protected static final String USER_RECEIVED_NODE = "received";

    private DatabaseHelper() {
        this(FirebaseFactory.getDatabase().getReference(),
             FirebaseFactory.getAuth());
    }
    
    /**
     * Package-private constructor for testing
     */
    DatabaseHelper(String databaseUrl) {
        this(
            FirebaseDatabase.getInstance(databaseUrl).getReference(),
            FirebaseAuth.getInstance()
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

        // Set sender and recipient emails
        message.setSenderId(mAuth.getCurrentUser().getUid());
        message.setSenderEmail(currentEmail);
        message.setRecipientId(getUserIdFromEmail(message.getRecipientEmail()));
        message.setRecipientEmail(message.getRecipientEmail());
        
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
