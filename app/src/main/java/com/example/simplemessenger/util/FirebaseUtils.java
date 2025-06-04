package com.example.simplemessenger.util;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.simplemessenger.R;
import com.example.simplemessenger.data.model.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Utility class for Firebase-related operations.
 */
public class FirebaseUtils {

    // Realtime Database paths
    public static final String PATH_USERS = "users";
    public static final String PATH_MESSAGES = "messages";
    public static final String PATH_USER_MESSAGES = "user-messages";
    
    // Storage paths
    private static final String STORAGE_PROFILE_IMAGES = "profile_images";
    private static final String STORAGE_MESSAGE_IMAGES = "message_images";
    private static final String STORAGE_MESSAGE_DOCUMENTS = "message_documents";
    
    private static FirebaseUtils instance;
    private final FirebaseAuth auth;
    private final DatabaseReference database;
    private final FirebaseStorage storage;
    
    private FirebaseUtils() {
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance("https://simplemessenger-c0a47-default-rtdb.europe-west1.firebasedatabase.app").getReference();
        storage = FirebaseStorage.getInstance();
    }
    
    /**
     * Get the singleton instance of FirebaseUtils.
     *
     * @return The FirebaseUtils instance.
     */
    public static synchronized FirebaseUtils getInstance() {
        if (instance == null) {
            instance = new FirebaseUtils();
        }
        return instance;
    }
    
    // Authentication methods
    
    /**
     * Get the current authenticated user.
     *
     * @return The current FirebaseUser, or null if not authenticated.
     */
    @Nullable
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }
    
    /**
     * Check if a user is currently signed in.
     *
     * @return true if a user is signed in, false otherwise.
     */
    public boolean isUserSignedIn() {
        return getCurrentUser() != null;
    }
    
    /**
     * Get the current user's ID.
     *
     * @return The current user's UID, or null if not authenticated.
     */
    @Nullable
    public String getCurrentUserId() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getUid() : null;
    }
    
    /**
     * Sign in with email and password.
     *
     * @param email    The user's email.
     * @param password The user's password.
     * @return A Task that completes with the authentication result.
     */
    public Task<FirebaseUser> signInWithEmail(String email, String password) {
        return auth.signInWithEmailAndPassword(email, password)
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        return task.getResult().getUser();
                    } else {
                        throw task.getException() != null ? task.getException() : 
                                new Exception("Failed to sign in with email");
                    }
                });
    }
    
    /**
     * Create a new user account with email and password.
     *
     * @param email    The user's email.
     * @param password The user's password.
     * @param name     The user's display name.
     * @return A Task that completes with the created user.
     */
    public Task<FirebaseUser> createUserWithEmail(String email, String password, String name) {
        return auth.createUserWithEmailAndPassword(email, password)
                .continueWithTask(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        FirebaseUser user = task.getResult().getUser();
                        if (user != null) {
                            // Update the user's profile with their name
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();
                            return user.updateProfile(profileUpdates)
                                    .continueWith(updateTask -> {
                                        if (updateTask.isSuccessful()) {
                                            return user;
                                        } else {
                                            throw updateTask.getException() != null ? 
                                                    updateTask.getException() : 
                                                    new Exception("Failed to update user profile");
                                        }
                                    });
                        }
                    }
                    throw task.getException() != null ? task.getException() : 
                            new Exception("Failed to create user account");
                });
    }
    
    /**
     * Sign in with a Google ID token.
     *
     * @param idToken The Google ID token.
     * @return A Task that completes with the authentication result.
     */
    public Task<FirebaseUser> signInWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        return auth.signInWithCredential(credential)
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        return task.getResult().getUser();
                    } else {
                        throw task.getException() != null ? task.getException() : 
                                new Exception("Failed to sign in with Google");
                    }
                });
    }
    
    /**
     * Sign in with a Facebook access token.
     *
     * @param token The Facebook access token.
     * @return A Task that completes with the authentication result.
     */
    public Task<FirebaseUser> signInWithFacebook(String token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token);
        return auth.signInWithCredential(credential)
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        return task.getResult().getUser();
                    } else {
                        throw task.getException() != null ? task.getException() : 
                                new Exception("Failed to sign in with Facebook");
                    }
                });
    }
    
    /**
     * Sign out the current user.
     */
    public void signOut() {
        auth.signOut();
    }
    
    /**
     * Send a password reset email.
     *
     * @param email The user's email address.
     * @return A Task that completes when the email is sent.
     */
    public Task<Void> sendPasswordResetEmail(String email) {
        return auth.sendPasswordResetEmail(email);
    }
    
    /**
     * Update the current user's email address.
     *
     * @param newEmail The new email address.
     * @return A Task that completes when the email is updated.
     */
    public Task<Void> updateEmail(String newEmail) {
        FirebaseUser user = getCurrentUser();
        if (user != null) {
            return user.updateEmail(newEmail);
        } else {
            throw new IllegalStateException("No user is currently signed in");
        }
    }
    
    /**
     * Update the current user's password.
     *
     * @param newPassword The new password.
     * @return A Task that completes when the password is updated.
     */
    public Task<Void> updatePassword(String newPassword) {
        FirebaseUser user = getCurrentUser();
        if (user != null) {
            return user.updatePassword(newPassword);
        } else {
            throw new IllegalStateException("No user is currently signed in");
        }
    }
    
    /**
     * Re-authenticate the user with their current password.
     *
     * @param password The user's current password.
     * @return A Task that completes when re-authentication is successful.
     */
    public Task<Void> reauthenticate(String password) {
        FirebaseUser user = getCurrentUser();
        if (user != null && user.getEmail() != null) {
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);
            return user.reauthenticate(credential);
        } else {
            throw new IllegalStateException("No user is currently signed in or email is null");
        }
    }
    
    /**
     * Delete the current user's account.
     *
     * @return A Task that completes when the account is deleted.
     */
    public Task<Void> deleteAccount() {
        FirebaseUser user = getCurrentUser();
        if (user != null) {
            return user.delete();
        } else {
            throw new IllegalStateException("No user is currently signed in");
        }
    }
    
    // User profile methods
    
    /**
     * Get a reference to the users node in Realtime Database.
     *
     * @return The users node reference.
     */
    public DatabaseReference getUsersReference() {
        return FirebaseDatabase.getInstance("https://simplemessenger-c0a47-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference(PATH_USERS);
    }
    
    /**
     * Get a reference to a specific user's data in Realtime Database.
     *
     * @param userId The user's ID.
     * @return The user's data reference.
     */
    public DatabaseReference getUserReference(String userId) {
        return getUsersReference().child(userId);
    }
    
    /**
     * Get a reference to the current user's data in Realtime Database.
     *
     * @return The current user's data reference, or null if not authenticated.
     */
    @Nullable
    public DatabaseReference getCurrentUserReference() {
        String userId = getCurrentUserId();
        return userId != null ? getUserReference(userId) : null;
    }
    
    /**
     * Get or create a user in Realtime Database.
     *
     * @param user The FirebaseUser.
     * @return A Task that completes with the user data snapshot.
     */
    public Task<DataSnapshot> getOrCreateUser(FirebaseUser user) {
        DatabaseReference userRef = getUserReference(user.getUid());
        return userRef.get().continueWithTask(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                return task;
            } else {
                // Create a new user entry
                Map<String, Object> userData = new HashMap<>();
                userData.put("id", user.getUid());
                userData.put("email", user.getEmail());
                userData.put("name", user.getDisplayName());
                userData.put("createdAt", System.currentTimeMillis());
                userData.put("updatedAt", System.currentTimeMillis());
                
                return userRef.updateChildren(userData).continueWithTask(task1 -> userRef.get());
            }
        });
    }
    
    /**
     * Update the current user's profile in Realtime Database.
     *
     * @param updates A map of fields to update.
     * @return A Task that completes when the update is done.
     */
    public Task<Void> updateUserProfile(Map<String, Object> updates) {
        DatabaseReference userRef = getCurrentUserReference();
        if (userRef != null) {
            updates.put("updatedAt", System.currentTimeMillis());
            return userRef.updateChildren(updates);
        } else {
            throw new IllegalStateException("No user is currently signed in");
        }
    }
    
    // Storage methods
    
    /**
     * Upload a profile image to Firebase Storage.
     *
     * @param userId The user's ID.
     * @param imageUri The URI of the image to upload.
     * @return A Task that completes with the download URL of the uploaded image.
     */
    public Task<Uri> uploadProfileImage(String userId, Uri imageUri) {
        if (TextUtils.isEmpty(userId) || imageUri == null) {
            throw new IllegalArgumentException("User ID and image URI cannot be null or empty");
        }
        
        String fileName = "profile_" + System.currentTimeMillis() + ".jpg";
        StorageReference storageRef = storage.getReference()
                .child(STORAGE_PROFILE_IMAGES)
                .child(userId)
                .child(fileName);
        
        return storageRef.putFile(imageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) {
                        throw task.getException();
                    }
                    return storageRef.getDownloadUrl();
                });
    }
    
    /**
     * Delete a profile image from Firebase Storage.
     *
     * @param imageUrl The URL of the image to delete.
     * @return A Task that completes when the deletion is done.
     */
    public Task<Void> deleteProfileImage(String imageUrl) {
        if (TextUtils.isEmpty(imageUrl)) {
            throw new IllegalArgumentException("Image URL cannot be null or empty");
        }
        
        StorageReference storageRef = storage.getReferenceFromUrl(imageUrl);
        return storageRef.delete();
    }
    
    // Helper methods
    
    /**
     * Get a formatted error message for a Firebase Authentication exception.
     *
     * @param context The context.
     * @param exception The exception.
     * @return A user-friendly error message.
     */
    public static String getAuthErrorMessage(Context context, Exception exception) {
        if (exception instanceof FirebaseAuthInvalidUserException) {
            return context.getString(R.string.error_user_not_found);
        } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            return context.getString(R.string.error_invalid_credentials);
        } else if (exception.getMessage() != null && exception.getMessage().contains("network error")) {
            return context.getString(R.string.error_network);
        } else {
            return exception.getMessage() != null ? 
                    exception.getMessage() : 
                    context.getString(R.string.error_unknown);
        }
    }
    
    /**
     * Get a reference to the messages node in Realtime Database.
     *
     * @return The messages node reference.
     */
    public DatabaseReference getMessagesReference() {
        return database.child(PATH_MESSAGES);
    }
    
    /**
     * Get a reference to a specific conversation's messages in Realtime Database.
     *
     * @param currentUserId The current user's ID.
     * @param otherUserId   The other user's ID.
     * @return A reference to the conversation's messages node.
     */
    public DatabaseReference getConversationReference(String currentUserId, String otherUserId) {
        String conversationId = generateConversationId(currentUserId, otherUserId);
        return getMessagesReference().child(conversationId);
    }
    
    /**
     * Get a query for messages between two users, ordered by timestamp.
     *
     * @param currentUserId The current user's ID.
     * @param otherUserId   The other user's ID.
     * @return A query for the conversation.
     */
    public Query getConversationQuery(String currentUserId, String otherUserId) {
        // Create a combined ID that's always the same regardless of the order of user IDs
        String conversationId = generateConversationId(currentUserId, otherUserId);
        
        return getMessagesReference()
                .child(conversationId)
                .orderByChild("timestamp");
    }
    
    /**
     * Generate a consistent conversation ID for two users.
     *
     * @param userId1 The first user's ID.
     * @param userId2 The second user's ID.
     * @return A consistent conversation ID.
     */
    public static String generateConversationId(String userId1, String userId2) {
        // Sort the user IDs to ensure the same conversation ID regardless of order
        if (userId1 == null || userId2 == null) {
            throw new IllegalArgumentException("User IDs cannot be null");
        }
        
        return userId1.compareTo(userId2) < 0 ? 
                userId1 + "_" + userId2 : 
                userId2 + "_" + userId1;
    }
    
    /**
     * Send a message to another user.
     *
     * @param senderId   The ID of the user sending the message.
     * @param receiverId The ID of the user receiving the message.
     * @param text       The message text.
     * @return A Task that completes with the DatabaseReference of the new message.
     */
    public Task<DatabaseReference> sendMessage(String senderId, String receiverId, String text) {
        if (TextUtils.isEmpty(senderId) || TextUtils.isEmpty(receiverId) || TextUtils.isEmpty(text)) {
            throw new IllegalArgumentException("Sender ID, receiver ID, and text cannot be empty");
        }
        
        String conversationId = generateConversationId(senderId, receiverId);
        String messageId = database.child(PATH_MESSAGES).push().getKey();
        if (messageId == null) {
            throw new IllegalStateException("Failed to generate message ID");
        }
        
        Map<String, Object> message = new HashMap<>();
        message.put("id", messageId);
        message.put("senderId", senderId);
        message.put("receiverId", receiverId);
        message.put("conversationId", conversationId);
        message.put("text", text);
        message.put("timestamp", ServerValue.TIMESTAMP);
        message.put("status", "sent");
        
        // Create updates for both the message and the user-messages references
        Map<String, Object> updates = new HashMap<>();
        updates.put("/" + PATH_MESSAGES + "/" + conversationId + "/" + messageId, message);
        
        // Also update the user-messages for both sender and receiver
        updates.put("/" + PATH_USER_MESSAGES + "/" + senderId + "/" + conversationId + "/" + messageId, true);
        updates.put("/" + PATH_USER_MESSAGES + "/" + receiverId + "/" + conversationId + "/" + messageId, true);
        
        return database.updateChildren(updates).continueWith(task -> {
            if (task.isSuccessful()) {
                return database.child(PATH_MESSAGES).child(conversationId).child(messageId);
            } else {
                throw task.getException();
            }
        });
    }
    
    /**
     * Mark a message as read.
     *
     * @param messageId The ID of the message to mark as read.
     * @param conversationId The ID of the conversation containing the message.
     * @return A Task that completes when the update is done.
     */
    public Task<Void> markMessageAsRead(String messageId, String conversationId) {
        if (TextUtils.isEmpty(messageId) || TextUtils.isEmpty(conversationId)) {
            throw new IllegalArgumentException("Message ID and conversation ID cannot be empty");
        }
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "read");
        updates.put("readAt", ServerValue.TIMESTAMP);
        
        return database.child(PATH_MESSAGES)
                .child(conversationId)
                .child(messageId)
                .updateChildren(updates);
    }
}
