package com.example.simplemessenger.ui.messaging;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.simplemessenger.R;
import com.example.simplemessenger.data.DatabaseHelper;
import com.example.simplemessenger.data.model.Message;
import com.example.simplemessenger.databinding.ActivityComposeMessageBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.example.simplemessenger.data.ContactsManager;
import com.example.simplemessenger.data.model.Contact;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComposeMessageActivity extends AppCompatActivity {

    private ActivityComposeMessageBinding binding;
    private DatabaseHelper databaseHelper;
    private FirebaseAuth mAuth;
    private ContactsManager contactsManager;
    private List<String> contactEmails = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityComposeMessageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase and DatabaseHelper
        mAuth = FirebaseAuth.getInstance();
        databaseHelper = DatabaseHelper.getInstance();
        contactsManager = ContactsManager.getInstance();
        
        // Set up the toolbar
        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.title_compose_message);
        }
        
        // Initialize contacts and set up auto-complete
        setupRecipientAutoComplete();
        loadContacts();
        
        // Set up click listeners
        binding.buttonSetReminder.setOnClickListener(v -> showReminderDialog());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_compose, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_send) {
            sendMessage();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    private void loadContacts() {
        String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            Log.e("ComposeMessage", "User not authenticated");
            return;
        }
        
        contactsManager.setLoadListener(new ContactsManager.ContactsLoadListener() {
            @Override
            public void onContactsLoaded(List<Contact> contacts) {
                Log.d("ComposeMessage", "Loaded " + contacts.size() + " contacts");
                contactEmails.clear();
                for (Contact contact : contacts) {
                    if (contact.getEmailAddress() != null && !contact.getEmailAddress().isEmpty()) {
                        contactEmails.add(contact.getEmailAddress());
                    }
                }
                updateAutoCompleteAdapter();
            }

            @Override
            public void onContactAdded(Contact contact) {
                if (contact.getEmailAddress() != null && !contact.getEmailAddress().isEmpty() && 
                    !contactEmails.contains(contact.getEmailAddress())) {
                    contactEmails.add(contact.getEmailAddress());
                    updateAutoCompleteAdapter();
                }
            }

            @Override
            public void onContactRemoved(Contact contact) {

            }

            @Override
            public void onError(String error) {
                Log.e("ComposeMessage", "Error loading contacts: " + error);
            }
        });
        
        // Load contacts from Firebase
        contactsManager.initializeContacts();
    }
    
    private void setupRecipientAutoComplete() {
        // The input_recipient is now an AutoCompleteTextView in the layout
        AutoCompleteTextView recipientInput = findViewById(R.id.input_recipient);
        
        if (recipientInput == null) {
            Log.e("ComposeMessage", "Could not find input_recipient AutoCompleteTextView");
            return;
        }
        
        // Set up the adapter for the AutoCompleteTextView
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                contactEmails
        );
        
        // Set the adapter
        recipientInput.setAdapter(adapter);
        
        // Set the threshold to 1 character
        recipientInput.setThreshold(1);
        
        // Add text changed listener to handle adding new contacts
        recipientInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                // You could add validation or other logic here
            }
        });
    }
    
    private void updateAutoCompleteAdapter() {
        AutoCompleteTextView recipientInput = findViewById(R.id.input_recipient);
        if (recipientInput != null) {
            ArrayAdapter<String> adapter = (ArrayAdapter<String>) recipientInput.getAdapter();
            if (adapter != null) {
                adapter.clear();
                adapter.addAll(contactEmails);
                adapter.notifyDataSetChanged();
            }
        }
    }
    
    private void showReminderDialog() {
        // TODO: Implement reminder dialog
        Toast.makeText(this, "Reminder functionality will be implemented here", Toast.LENGTH_SHORT).show();
    }

    private void sendMessage() {
        Log.d("SendMessage", "Starting sendMessage");
        String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        
        if (currentUserId == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }

        String recipientEmail = binding.inputRecipient.getText().toString().trim();
        String subject = binding.inputSubject.getText().toString().trim();
        String messageText = binding.inputMessage.getText().toString().trim();

        // Check if recipient exists in contacts
        Contact recipientContact = contactsManager.getContactByEmail(recipientEmail);
        if (recipientContact == null) {
            // Create new contact
            contactsManager.addContact(recipientEmail, recipientEmail, recipientEmail);
        }

        // Create message
        Message message = new Message();
        message.setSenderId(currentUserId);
        message.setRecipientEmail(recipientEmail);
        message.setSubject(subject);
        message.setContent(messageText);
        message.setTimestamp(System.currentTimeMillis());
        message.setRead(false);
        message.setArchived(false);

        // Save message
        databaseHelper.sendMessage(message, new DatabaseHelper.DatabaseCallback() {
            @Override
            public void onSuccess(Object result) {
                Toast.makeText(ComposeMessageActivity.this, "Message sent successfully", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(ComposeMessageActivity.this, "Error sending message: " + error, Toast.LENGTH_SHORT).show();
            }
        });

        // Validate inputs
        if (TextUtils.isEmpty(recipientEmail)) {
            binding.inputRecipient.setError(getString(R.string.error_field_required));
            binding.inputRecipient.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(messageText)) {
            binding.inputMessage.setError(getString(R.string.error_field_required));
            binding.inputMessage.requestFocus();
            return;
        }

        // Get current user
//        String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        String currentUserEmail = mAuth.getCurrentUser() != null ? 
                mAuth.getCurrentUser().getEmail() : "Unknown";

        if (currentUserId == null) {
            Toast.makeText(this, R.string.error_authentication_required, Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state
        showLoading(true);

        // First, look up the recipient's UID by email
        DatabaseReference usersRef = databaseHelper.getDatabaseReference().child("users");
        Log.d("SendMessage", "Querying users at: " + usersRef.toString());
        
        usersRef.orderByChild("email")
                .equalTo(recipientEmail)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        Log.d("SendMessage", "onDataChange, exists: " + dataSnapshot.exists());
                        if (!dataSnapshot.exists()) {
                            // No user found with that email
                            showLoading(false);
                            binding.inputRecipient.setError("No user found with this email");
                            Log.d("SendMessage", "No user found with email: " + recipientEmail);
                            return;
                        }

                        // Get the first matching user (should be only one)
                        DataSnapshot userSnapshot = dataSnapshot.getChildren().iterator().next();
                        String recipientId = userSnapshot.getKey();
                        
                        if (recipientId == null) {
                            handleError("Invalid recipient");
                            return;
                        }

                        // Create message ID
                        String messageId = databaseHelper.getDatabaseReference().child("messages").push().getKey();
                        if (messageId == null) {
                            handleError("Error creating message");
                            return;
                        }

                        long timestamp = System.currentTimeMillis();
                        
                        // Create message map according to the rules
                        Map<String, Object> messageMap = new HashMap<>();
                        messageMap.put("senderId", currentUserId);
                        messageMap.put("recipientId", recipientId);
                        messageMap.put("message", messageText);
                        messageMap.put("subject", subject);
                        messageMap.put("timestamp", timestamp);
                        messageMap.put("read", false);

                        // Create updates map for atomic updates
                        Map<String, Object> updates = new HashMap<>();
                        
                        // Add to /messages
                        updates.put("/messages/" + messageId, messageMap);
                        
                        // Add to sender's sent messages
                        updates.put("/user-messages/" + currentUserId + "/sent/" + messageId, true);
                        
                        // Add to recipient's received messages
                        updates.put("/user-messages/" + recipientId + "/received/" + messageId, true);

                        // Execute updates
                        Log.d("SendMessage", "Attempting to update with: " + updates.toString());
                        databaseHelper.getDatabaseReference().updateChildren(updates)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("SendMessage", "Message sent successfully");
                                    Log.d("ComposeMessage", "Message sent successfully");
                                    runOnUiThread(() -> {
                                        showLoading(false);
                                        Toast.makeText(ComposeMessageActivity.this,
                                                R.string.message_sent, 
                                                Toast.LENGTH_SHORT).show();
                                        finish();
                                    });
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("SendMessage", "Error updating database", e);
                                    handleError("Failed to send message: Firebase Database error: " + 
                                            (e != null ? e.getMessage() : "Unknown error"));
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        handleError("Error: " + databaseError.getMessage());
                    }
                    
                    private void handleError(String error) {
                        runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(ComposeMessageActivity.this,
                                    error,
                                    Toast.LENGTH_LONG).show();
                        });
                        Log.e("ComposeMessage", error);
                    }
                });
    }
    
    private void showLoading(boolean isLoading) {
        if (isLoading) {
            // Disable UI elements
            binding.inputRecipient.setEnabled(false);
            binding.inputSubject.setEnabled(false);
            binding.inputMessage.setEnabled(false);
            binding.checkboxSetReminder.setEnabled(false);
            binding.buttonSetReminder.setEnabled(false);
            
            // Show a toast to indicate loading
            Toast.makeText(this, "Sending message...", Toast.LENGTH_SHORT).show();
        } else {
            // Re-enable UI elements
            binding.inputRecipient.setEnabled(true);
            binding.inputSubject.setEnabled(true);
            binding.inputMessage.setEnabled(true);
            binding.checkboxSetReminder.setEnabled(true);
            binding.buttonSetReminder.setEnabled(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
