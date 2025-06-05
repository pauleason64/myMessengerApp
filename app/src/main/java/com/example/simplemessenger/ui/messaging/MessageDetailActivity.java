package com.example.simplemessenger.ui.messaging;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.simplemessenger.data.DatabaseHelper;
import com.example.simplemessenger.data.model.Message;
import com.example.simplemessenger.databinding.ActivityMessageDetailBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.example.simplemessenger.data.ContactsManager;
import com.example.simplemessenger.data.model.Contact;

import java.util.List;

public class MessageDetailActivity extends AppCompatActivity {

    private ActivityMessageDetailBinding binding;
    private DatabaseHelper databaseHelper;
    private FirebaseAuth mAuth;
    private String messageId;
    private Message message;
    private ContactsManager contactsManager;
    private ContactsManager.ContactsLoadListener contactListener;
    private MessageDetailUiHelper uiHelper;
    private ValueEventListener messageValueEventListener;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMessageDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set up the toolbar
        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        }

        // Get message ID from intent
        messageId = getIntent().getStringExtra("MESSAGE_ID");
        if (messageId == null || messageId.isEmpty()) {
            showError("No message ID provided");
            finish();
            return;
        }

        // Initialize UI helper
        uiHelper = new MessageDetailUiHelper(this, binding);

        // Initialize database helper using getInstance()
        databaseHelper = DatabaseHelper.getInstance();

        // Initialize contacts manager using getInstance()
        contactsManager = ContactsManager.getInstance();

        // Load message details
        loadMessageDetails();
    }


    private void loadMessageDetails() {
        if (messageId == null || messageId.isEmpty()) {
            showError("Invalid message ID");
            finish();
            return;
        }

        // Get message from local database first
        databaseHelper.getMessage(messageId, new DatabaseHelper.DatabaseCallback() {
            @Override
            public void onSuccess(Object result) {
                if (result instanceof Message) {
                    message = (Message) result;
                    // Update UI with cached message
                    runOnUiThread(() -> updateUI());
                }
            }

            @Override
            public void onError(String error) {
                Log.e("MessageDetail", "Error loading message from database: " + error);
            }
        });

        // Then try to get the latest from Firebase
        DatabaseReference messageRef = FirebaseDatabase.getInstance().getReference("messages").child(messageId);
        messageValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    message = dataSnapshot.getValue(Message.class);
                    if (message != null) {
                        message.setId(dataSnapshot.getKey());
                        
                        // Save to local database - This needs to be implemented in DatabaseHelper
                        // databaseHelper.saveMessage(message);
                        
                        // Mark as read if not already read
                        if (!message.isRead()) {
                            markAsRead();
                        }
                        
                        // Update UI
                        updateUI();
                        
                        // Load contact information
                        loadContactInformation();
                    }
                } else {
                    showError("Message not found");
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                showError("Failed to load message: " + databaseError.getMessage());
                finish();
            }
        };
        
        messageRef.addListenerForSingleValueEvent(messageValueEventListener);
    }

    private void loadContactInformation() {
        if (message == null || message.getIsNote()) {
            return;
        }

        // Get sender and recipient IDs
        final String messageId = message.getId();
        final String senderId = message.getSenderId();
        final String recipientId = message.getRecipientId();
        final String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        // Set up contact listener
        contactListener = new ContactsManager.ContactsLoadListener() {
            @Override
            public void onContactsLoaded(List<Contact> contacts) {
                // Resolve contacts from cache
                resolveContactInfo(message, currentUserId, senderId, recipientId);
                
                // Fetch any missing contacts
                if (senderId != null && !senderId.equals(currentUserId) && 
                    (message.getSenderEmail() == null || message.getSenderEmail().isEmpty())) {
                    fetchMissingContact(senderId, true);
                }
                
                if (recipientId != null && !recipientId.equals(currentUserId) && 
                    (message.getRecipientEmail() == null || message.getRecipientEmail().isEmpty())) {
                    fetchMissingContact(recipientId, false);
                }
            }

            @Override
            public void onContactAdded(Contact contact) {
                if (contact == null || contact.getUserId() == null) {
                    return;
                }
                
                // Update the contact info if it's relevant to this message
                updateContactInfo(contact);
            }


            @Override
            public void onContactRemoved(Contact contact) {
                // Not handling contact removal in this view
            }

            @Override
            public void onError(String error) {
                Log.e("MessageDetail", "Contacts error: " + error);
                runOnUiThread(() -> showError("Error loading contacts"));
            }
        };

        // Set the listener and clear previous one
        contactsManager.setLoadListener(contactListener);
        contactsManager.clearPreviousListener();

        // For notes, we don't need to load contacts
        if (message.getIsNote()) {
            updateUI();
            return;
        }

        // For sent items, we know the sender is the current user
        if (currentUserId != null && currentUserId.equals(senderId)) {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null && currentUser.getEmail() != null) {
                message.setSenderEmail(currentUser.getEmail());
            }
        }
        
        // For received items, we know the recipient is the current user
        if (currentUserId != null && currentUserId.equals(recipientId)) {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null && currentUser.getEmail() != null) {
                message.setRecipientEmail(currentUser.getEmail());
            }
        }

        // Initialize contacts to load any missing contact info
        contactsManager.initializeContacts();
        
        // Initial resolution from cache
        resolveContactInfo(message, currentUserId, senderId, recipientId);
    }

    private void updateUI() {
        if (uiHelper != null && message != null) {
            uiHelper.updateUI(message);
        }
    }
    
    /**
     * Resolves contact information from the cache and updates the message
     * @param message The message to update
     * @param currentUserId The current user's ID
     * @param senderId The sender's user ID
     * @param recipientId The recipient's user ID
     */
    private void resolveContactInfo(Message message, String currentUserId, String senderId, String recipientId) {
        if (message == null || contactsManager == null) {
            return;
        }
        
        boolean updated = false;
        
        // Resolve sender info
        if (senderId != null && !senderId.equals(currentUserId)) {
            Contact sender = contactsManager.getContactById(senderId);
            if (sender != null && sender.getEmailAddress() != null) {
                message.setSenderEmail(sender.getEmailAddress());
                updated = true;
                Log.d("MessageDetail", "Resolved sender from cache: " + sender.getEmailAddress());
            }
        }
        
        // Resolve recipient info
        if (recipientId != null && !recipientId.equals(currentUserId)) {
            Contact recipient = contactsManager.getContactById(recipientId);
            if (recipient != null && recipient.getEmailAddress() != null) {
                message.setRecipientEmail(recipient.getEmailAddress());
                updated = true;
                Log.d("MessageDetail", "Resolved recipient from cache: " + recipient.getEmailAddress());
            }
        }
        
        // Update UI if we made any changes
        if (updated) {
            runOnUiThread(this::updateUI);
        }
    }
    
    /**
     * Updates contact information when a contact is added or loaded
     * @param contact The contact that was added or loaded
     */
    private void updateContactInfo(Contact contact) {
        if (contact == null || contact.getUserId() == null) {
            Log.d("MessageDetail", "Skipping updateContactInfo - null contact or userId");
            return;
        }
        
        boolean updated = false;
        if (contact.getUserId().equals(message.getSenderId())) {
            message.setSenderEmail(contact.getEmailAddress());
            updated = true;
            Log.d("MessageDetail", "Updated sender email: " + contact.getEmailAddress());
        } else if (contact.getUserId().equals(message.getRecipientId())) {
            message.setRecipientEmail(contact.getEmailAddress());
            updated = true;
            Log.d("MessageDetail", "Updated recipient email: " + contact.getEmailAddress());
        }
        
        if (updated) {
            runOnUiThread(this::updateUI);
        }
    }
    
    /**
     * Fetches a contact from the server if not in cache
     * @param userId The user ID to fetch
     * @param isSender Whether this is the sender (true) or recipient (false)
     */
    private void fetchMissingContact(String userId, boolean isSender) {
        if (userId == null || contactsManager == null) {
            return;
        }
        
        contactsManager.fetchAndCreateContact(userId, new ContactsManager.ContactsLoadListener() {
            @Override
            public void onContactAdded(Contact contact) {
                if (contact != null && contact.getUserId().equals(userId)) {
                    String email = contact.getEmailAddress();
                    if (isSender) {
                        message.setSenderEmail(email);
                    } else {
                        message.setRecipientEmail(email);
                    }
                    Log.d("MessageDetail", "Fetched contact: " + email);
                    runOnUiThread(() -> updateUI());
                }
            }
            
            @Override public void onContactsLoaded(List<Contact> contacts) {}
            @Override public void onContactRemoved(Contact contact) {}
            @Override public void onError(String error) {
                Log.e("MessageDetail", "Error fetching contact: " + error);
            }
        });
    }

    private void markAsRead() {
        if (message != null && !message.isRead()) {
            message.setRead(true);
            // Update read status in Firebase
            FirebaseDatabase.getInstance().getReference("messages")
                .child(message.getId())
                .child("read")
                .setValue(true);
        }
    }

    private void showError(String message) {
        Log.e("MessageDetail", message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove listeners to prevent memory leaks
        if (messageValueEventListener != null) {
            FirebaseDatabase.getInstance().getReference("messages")
                .child(messageId)
                .removeEventListener(messageValueEventListener);
        }
        
        if (contactsManager != null) {
            contactsManager.cleanupContacts();
        }
    }
    
    // ... rest of the methods remain unchanged ...
}
