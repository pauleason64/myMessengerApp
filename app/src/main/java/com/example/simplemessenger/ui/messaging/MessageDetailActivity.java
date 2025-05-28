package com.example.simplemessenger.ui.messaging;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.simplemessenger.R;
import com.example.simplemessenger.SimpleMessengerApp;
import com.example.simplemessenger.data.DatabaseHelper;
import com.example.simplemessenger.data.model.Message;
import com.example.simplemessenger.databinding.ActivityMessageDetailBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.example.simplemessenger.data.ContactsManager;
import com.example.simplemessenger.data.model.Contact;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageDetailActivity extends AppCompatActivity {

    private ActivityMessageDetailBinding binding;
    private DatabaseHelper databaseHelper;
    private FirebaseAuth mAuth;
    private String messageId;
    private Message message;
    private ContactsManager contactsManager;
    private ContactsManager.ContactsLoadListener contactListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMessageDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Get message ID from intent
        messageId = getIntent().getStringExtra("message_id");
        if (messageId == null) {
            finish();
            return;
        }

        // Initialize Firebase instances and ContactsManager
        databaseHelper = DatabaseHelper.getInstance();
        mAuth = FirebaseAuth.getInstance();
        contactsManager = ContactsManager.getInstance();

        // Start contact updates
        contactsManager.initializeContacts();

        // Set up the toolbar
        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }

        // Set up FAB for reply
        binding.fabReply.setOnClickListener(v -> replyToMessage());
        binding.fabForward.setOnClickListener(v -> forwardMessage());
        binding.fabDelete.setOnClickListener(v -> showDeleteConfirmation());

        // Load message details
        loadMessageDetails();
    }


    private void loadMessageDetails() {
        if (messageId == null) {
            return;
        }

        DatabaseReference messageRef = databaseHelper.getDatabaseReference().child("messages").child(messageId);
        messageRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Log the raw data from Firebase
                    Log.d("MessageDetail", "Raw message data: " + dataSnapshot.getValue());
                    
                    // Get the message data
                    message = dataSnapshot.getValue(Message.class);
                    if (message != null) {
                        message.setId(dataSnapshot.getKey());
                        
                        // Get sender and recipient IDs
                        String senderId = dataSnapshot.child("senderId").getValue(String.class);
                        String recipientId = dataSnapshot.child("recipientId").getValue(String.class);
                        
                        Log.d("MessageDetail", "Sender ID: " + senderId + ", Recipient ID: " + recipientId);
                        
                        // Initialize contact listener if not already created
                        if (contactListener == null) {
                            contactListener = new ContactsManager.ContactsLoadListener() {
                                @Override
                                public void onContactsLoaded(List<Contact> contacts) {
                                    Log.d("MessageDetail", "Contacts loaded: " + contacts.size() + " contacts");
                                    // Update UI with any existing contacts
                                    for (Contact contact : contacts) {
                                        if (contact.getUserId().equals(senderId)) {
                                            message.setSenderEmail(contact.getEmailAddress());
                                            runOnUiThread(() -> updateUI());
                                        } else if (contact.getUserId().equals(recipientId)) {
                                            message.setRecipientEmail(contact.getEmailAddress());
                                            runOnUiThread(() -> updateUI());
                                        }
                                    }
                                }

                                @Override
                                public void onContactAdded(Contact contact) {
                                    Log.d("MessageDetail", "Contact added: " + contact.getEmailAddress());
                                    if (contact.getUserId().equals(senderId)) {
                                        message.setSenderEmail(contact.getEmailAddress());
                                        runOnUiThread(() -> updateUI());
                                    } else if (contact.getUserId().equals(recipientId)) {
                                        message.setRecipientEmail(contact.getEmailAddress());
                                        runOnUiThread(() -> updateUI());
                                    }
                                }

                                @Override
                                public void onContactRemoved(Contact contact) {
                                    Log.d("MessageDetail", "Contact removed: " + contact.getEmailAddress());
                                    // Not needed here
                                }

                                @Override
                                public void onError(String error) {
                                    Log.e("MessageDetail", "Error loading contact: " + error);
                                }
                            };
                        }
                        
                        // Set the listener and clear previous one
                        contactsManager.setLoadListener(contactListener);
                        contactsManager.clearPreviousListener();

                        // First check if we have the emails from the message
                        if (message.getSenderEmail() != null && !message.getSenderEmail().isEmpty() &&
                            message.getRecipientEmail() != null && !message.getRecipientEmail().isEmpty()) {
                            // We have all emails, no need to load contacts
                            updateUI();
                            return;
                        }

                        // Load all contacts for the current user
                        contactsManager.initializeContacts();

                        // Wait a moment for contacts to load before fetching
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            // Fetch and create sender contact if not found
                            if (senderId != null && !senderId.isEmpty() && 
                                (message.getSenderEmail() == null || message.getSenderEmail().isEmpty())) {
                                contactsManager.fetchAndCreateContact(senderId, contactsManager.getLoadListener());
                            }

                            // Fetch and create recipient contact if not found
                            if (recipientId != null && !recipientId.isEmpty() && 
                                (message.getRecipientEmail() == null || message.getRecipientEmail().isEmpty())) {
                                contactsManager.fetchAndCreateContact(recipientId, contactsManager.getLoadListener());
                            }
                        }, 500);

                        // Set initial values from message data
                        message.setSenderEmail(message.getSenderEmail());
                        message.setRecipientEmail(message.getRecipientEmail());
                        updateUI();

                        // Only fetch contacts if we don't have the emails yet
                        if (message.getSenderEmail() == null || message.getSenderEmail().isEmpty() ||
                            message.getRecipientEmail() == null || message.getRecipientEmail().isEmpty()) {
                            
                            // Set up a listener for contact updates
                            ContactsManager.ContactsLoadListener contactListener = new ContactsManager.ContactsLoadListener() {
                                @Override
                                public void onContactsLoaded(List<Contact> contacts) {
                                    // Not needed here
                                }

                                @Override
                                public void onContactAdded(Contact contact) {
                                    if (contact.getUserId().equals(senderId)) {
                                        message.setSenderEmail(contact.getEmailAddress());
                                        runOnUiThread(() -> updateUI());
                                    } else if (contact.getUserId().equals(recipientId)) {
                                        message.setRecipientEmail(contact.getEmailAddress());
                                        runOnUiThread(() -> updateUI());
                                    }
                                }

                                @Override
                                public void onContactRemoved(Contact contact) {
                                    // Not needed here
                                }

                                @Override
                                public void onError(String error) {
                                    Log.e("MessageDetail", "Error loading contact: " + error);
                                }
                            };
                            
                            // Set the listener and clear previous one
                            contactsManager.setLoadListener(contactListener);
                            contactsManager.clearPreviousListener();

                            // Fetch and create sender contact
                            if (senderId != null && !senderId.isEmpty()) {
                                contactsManager.fetchAndCreateContact(senderId, contactsManager.getLoadListener());
                            }

                            // Fetch and create recipient contact
                            if (recipientId != null && !recipientId.isEmpty()) {
                                contactsManager.fetchAndCreateContact(recipientId, contactsManager.getLoadListener());
                            }
                        }                  runOnUiThread(() -> updateUI());

                        // Mark as read if not already read
                        if (!message.isRead()) {
                            markAsRead();
                        }
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
        });
    }

    private void updateUI() {
        if (message == null || binding == null) {
            return;
        }

        // Update toolbar title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(message.getSubject());
        }

        // Update message details
        runOnUiThread(() -> {
            if (binding != null) {
                // Add From: and To: labels
                binding.textFrom.setText(getString(R.string.label_from, message.getSenderEmail()));
                binding.textTo.setText(getString(R.string.label_to, message.getRecipientEmail()));
                binding.textSubject.setText(message.getSubject());
                binding.textMessage.setText(message.getContent());
                
                // Format and set timestamp
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
                binding.textDate.setText(sdf.format(new Date(message.getTimestamp())));
                
                // Handle reminder
                if (message.isHasReminder() && message.getReminderTime() > 0) {
                    SimpleDateFormat reminderFormat = new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault());
                    String reminderText = getString(R.string.reminder_set_for, 
                            reminderFormat.format(new Date(message.getReminderTime())));
                    binding.layoutReminder.setVisibility(View.VISIBLE);
                    binding.textReminderTime.setText(reminderText);
                } else {
                    binding.layoutReminder.setVisibility(View.GONE);
                }
            }
        });
    }

    private void markAsRead() {
        if (messageId == null) {
            return;
        }

        databaseHelper.getDatabaseReference().child("messages").child(messageId).child("read")
                .setValue(true)
                .addOnSuccessListener(aVoid -> {
                    if (message != null) {
                        message.setRead(true);
                    }
                });
    }

    private void replyToMessage() {
        if (message == null) {
            return;
        }

        Intent intent = new Intent(this, ComposeMessageActivity.class);
        intent.putExtra("reply_to", message.getSenderEmail());
        intent.putExtra("subject", getString(R.string.reply_prefix) + message.getSubject());
        startActivity(intent);
    }

    private void forwardMessage() {
        if (message == null) {
            return;
        }

        Intent intent = new Intent(this, ComposeMessageActivity.class);
        intent.putExtra("subject", getString(R.string.forward_prefix) + message.getSubject());
        intent.putExtra("message", message.getContent());
        startActivity(intent);
    }

    private void showDeleteConfirmation() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_message)
                .setMessage(R.string.delete_message_confirmation)
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteMessage())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void deleteMessage() {
        if (messageId == null) {
            return;
        }

        // In a real app, you might want to move to trash instead of deleting permanently
        databaseHelper.getDatabaseReference().child("messages").child(messageId).child("archived")
                .setValue(true)
                .addOnSuccessListener(aVoid -> {
                    // Also update in user-messages
                    if (mAuth.getCurrentUser() != null) {
                        databaseHelper.getDatabaseReference().child("user-messages")
                                .child(mAuth.getCurrentUser().getUid())
                                .child(messageId)
                                .child("archived")
                                .setValue(true);
                    }
                    Toast.makeText(this, R.string.message_deleted, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    showError("Failed to delete message: " + e.getMessage());
                });
    }

    private void fetchUserEmail(String userId, boolean isSender) {
        if (userId == null || userId.isEmpty()) {
            Log.e("MessageDetail", "Invalid user ID provided");
            return;
        }

        // First check if the current user is trying to fetch their own email
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && userId.equals(currentUser.getUid())) {
            // This is the current user, use their email
            String email = currentUser.getEmail();
            if (isSender) {
                message.setSenderEmail(email);
            } else {
                message.setRecipientEmail(email);
            }
            updateUI();
            return;
        }

        DatabaseReference userRef = databaseHelper.getDatabaseReference()
                .child("users")
                .child(userId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String email = dataSnapshot.child("email").getValue(String.class);
                    String displayName = dataSnapshot.child("displayName").getValue(String.class);
                    
                    if (email != null && !email.isEmpty()) {
                        // Update the message with the email
                        if (isSender) {
                            message.setSenderEmail(email);
                            Log.d("MessageDetail", "Fetched sender email: " + email);
                        } else {
                            message.setRecipientEmail(email);
                            Log.d("MessageDetail", "Fetched recipient email: " + email);
                        }
                        
                        // Add this user to contacts if they're not already there
                        if (displayName == null || displayName.isEmpty()) {
                            displayName = email.split("@")[0]; // Use the part before @ as display name if no display name
                        }
                        
                        // Add to contacts if not already present
                        Contact existingContact = contactsManager.getContactById(userId);
                        if (existingContact == null) {
                            contactsManager.addContact(userId, displayName, email);
                            Log.d("MessageDetail", "Added new contact: " + displayName + " <" + email + ">");
                            
                            // Update the UI after a short delay to allow the contact to be saved
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                contactsManager.initializeContacts(); // Refresh contacts
                                updateUI();
                            }, 500);
                        } else {
                            updateUI();
                        }
                    } else {
                        Log.e("MessageDetail", "Email not found for user: " + userId);
                        updateUI();
                    }
                } else {
                    Log.e("MessageDetail", "User not found with ID: " + userId);
                    updateUI();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("MessageDetail", "Error fetching user data: " + databaseError.getMessage());
                updateUI();
            }
        });
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_message_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_archive) {
            // TODO: Implement archive action
            Toast.makeText(this, "Archive message", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_set_reminder) {
            // TODO: Implement set reminder action
            Toast.makeText(this, "Set reminder", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_share) {
            // TODO: Implement share action
            shareMessage();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    private void shareMessage() {
        if (message == null) {
            return;
        }

        String shareText = message.getSubject() + "\n\n" + message.getContent();
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, message.getSubject());
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_message)));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
