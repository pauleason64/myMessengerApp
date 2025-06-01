package com.example.simplemessenger.ui.messaging;

import android.content.Context;
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
import android.os.Build;
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
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import com.example.simplemessenger.data.ContactsManager;
import com.example.simplemessenger.data.model.Contact;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComposeMessageActivity extends AppCompatActivity {
    private static final String TAG = "ComposeMessage";

    // Intent extra keys
    public static final String EXTRA_MESSAGE_ID = "message_id";
    public static final String EXTRA_FORWARD_MESSAGE_ID = "forward_message_id";
    public static final String EXTRA_SUBJECT = "subject";
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_REPLY_TO = "reply_to";
    public static final String EXTRA_IS_NOTE = "is_note";
    public static final String EXTRA_COMPOSE_NEW = "compose_new";
    public static final String EXTRA_NOTE_MODE = "note_mode";

    private ActivityComposeMessageBinding binding;
    private DatabaseHelper databaseHelper;
    private FirebaseAuth mAuth;
    private ContactsManager contactsManager;
    private List<String> contactEmails = new ArrayList<>();
    private boolean isNoteMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityComposeMessageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Handle intent extras
        Intent intent = getIntent();
        if (intent != null) {
            // Check for note mode from intent - any of these flags can indicate note mode
            isNoteMode = intent.getBooleanExtra(EXTRA_IS_NOTE, false) || 
                        intent.getBooleanExtra(EXTRA_NOTE_MODE, false);
            
            boolean composeNew = intent.getBooleanExtra(EXTRA_COMPOSE_NEW, false);
            
            Log.d(TAG, "onCreate - isNoteMode: " + isNoteMode + ", composeNew: " + composeNew);
            
            // If we're composing a new message/note, clear any existing data
            if (composeNew) {
                binding.inputSubject.setText("");
                binding.inputMessage.setText("");
                if (!isNoteMode) { // Only clear recipient if not in note mode
                    binding.inputRecipient.setText("");
                }
                
                // Set focus to the appropriate field
                if (isNoteMode) {
                    binding.inputSubject.requestFocus();
                } else {
                    binding.inputRecipient.requestFocus();
                }
            }
            
            // Handle reply subject
            if (intent.hasExtra(EXTRA_REPLY_TO)) {
                String replyTo = intent.getStringExtra(EXTRA_REPLY_TO);
                if (replyTo != null && !replyTo.isEmpty()) {
                    binding.inputRecipient.setText(replyTo);
                }
            }
            
            // Handle subject
            if (intent.hasExtra(EXTRA_SUBJECT)) {
                String subject = intent.getStringExtra(EXTRA_SUBJECT);
                if (subject != null && !subject.isEmpty()) {
                    binding.inputSubject.setText(subject);
                }
            }
        }

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
            getSupportActionBar().setTitle(isNoteMode ? R.string.title_compose_note : R.string.title_compose_message);
        }
        
        // Initialize contacts first
        loadContacts();
        
        // Set up auto-complete after contacts are loaded
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
                setupRecipientAutoComplete();  // Set up autocomplete after contacts are loaded
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
                if (contact.getEmailAddress() != null && contact.getEmailAddress().isEmpty()) {
                    contactEmails.remove(contact.getEmailAddress());
                    updateAutoCompleteAdapter();
                }
            }

            @Override
            public void onError(String error) {
                Log.e("ComposeMessage", "Error loading contacts: " + error);
            }
        });
        
        // Load contacts from Firebase
        contactsManager.initializeContacts();
        
        // Set up click listeners
        binding.fabSend.setOnClickListener(v -> {
            if (isNoteMode) {
                saveNote();
            } else {
                sendMessage();
            }
        });
        
        // Set up the initial UI state
        updateUiForMode();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_compose, menu);
        
        // Set up the note/message toggle
        MenuItem noteItem = menu.findItem(R.id.action_toggle_note);
        if (noteItem != null) {
            // Set the initial state based on current mode
            updateNoteToggleMenuItem(noteItem);
        }
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_send) {
            if (isNoteMode) {
                saveNote();
            } else {
                sendMessage();
            }
            return true;
        } else if (id == R.id.action_toggle_note) {
            // Toggle between note and message mode
            isNoteMode = !isNoteMode;
            
            // Clear recipient field when switching to note mode
            if (isNoteMode) {
                binding.inputRecipient.setText("");
            }
            
            // Update UI
            updateUiForMode();
            
            // Update the menu item
            updateNoteToggleMenuItem(item);
            
            // Show a toast to indicate the current mode
            Toast.makeText(this, 
                isNoteMode ? R.string.note_mode_enabled : R.string.message_mode_enabled,
                Toast.LENGTH_SHORT).show();
                
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * Updates the note toggle menu item based on the current mode
     * @param menuItem The menu item to update
     */
    private void updateNoteToggleMenuItem(MenuItem menuItem) {
        if (menuItem == null) return;
        
        // Update the title and icon based on the current mode
        menuItem.setTitle(isNoteMode ? R.string.message_mode : R.string.note_mode);
        menuItem.setIcon(isNoteMode ? R.drawable.ic_message : R.drawable.ic_note);
        
        // Set content description for accessibility (API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            menuItem.setContentDescription(isNoteMode ? 
                getString(R.string.switch_to_message_mode) : 
                getString(R.string.switch_to_note_mode));
        }
    }
    
    /**
     * Updates the UI based on the current mode (note or message)
     */
    /**
     * Updates the UI based on the current mode (note or message)
     */
    private void updateUiForMode() {
        if (binding == null) {
            Log.w(TAG, "updateUiForMode: Binding is null, skipping UI update");
            return;
        }
        
        Log.d(TAG, "Updating UI for mode - isNoteMode: " + isNoteMode);
        
        if (isNoteMode) {
            // Update window title
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.title_compose_note);
            }
            
            // Hide recipient field for notes
            binding.layoutRecipient.setVisibility(View.GONE);
            
            // Update hints for note mode
            binding.layoutSubject.setHint(R.string.hint_note_title);
            binding.inputMessage.setHint(R.string.hint_note_content);
            
            // Update send button icon and behavior
            binding.fabSend.setImageResource(R.drawable.ic_save);
            binding.fabSend.setContentDescription(getString(R.string.save));
            
            // Clear any recipient-related errors
            binding.inputRecipient.setError(null);
            
            // Set focus to subject if empty, otherwise to message
            if (TextUtils.isEmpty(binding.inputSubject.getText())) {
                binding.inputSubject.requestFocus();
            } else if (TextUtils.isEmpty(binding.inputMessage.getText())) {
                binding.inputMessage.requestFocus();
            }
        } else {
            // Update window title
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.title_compose_message);
            }
            
            // Show recipient field for messages
            binding.layoutRecipient.setVisibility(View.VISIBLE);
            
            // Restore hints for message mode
            binding.layoutSubject.setHint(R.string.hint_subject);
            binding.inputMessage.setHint(R.string.hint_message);
            
            // Restore send button icon
            binding.fabSend.setImageResource(R.drawable.ic_send);
            binding.fabSend.setContentDescription(getString(R.string.send_message));
            
            // Set focus to the first empty field
            if (TextUtils.isEmpty(binding.inputRecipient.getText())) {
                binding.inputRecipient.requestFocus();
            } else if (TextUtils.isEmpty(binding.inputSubject.getText())) {
                binding.inputSubject.requestFocus();
            } else if (TextUtils.isEmpty(binding.inputMessage.getText())) {
                binding.inputMessage.requestFocus();
            }
        }
        
        // Invalidate options menu to update the note/message toggle
        invalidateOptionsMenu();
        
        // Log the current UI state
        Log.d(TAG, "UI updated - isNoteMode: " + isNoteMode + 
              ", recipient visible: " + (binding.layoutRecipient.getVisibility() == View.VISIBLE) +
              ", subject hint: " + binding.layoutSubject.getHint() +
              ", message hint: " + binding.inputMessage.getHint());
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

    private void saveNote() {
        String title = binding.inputSubject.getText().toString().trim();
        String content = binding.inputMessage.getText().toString().trim();
        
        if (TextUtils.isEmpty(content)) {
            binding.inputMessage.setError(getString(R.string.error_message_required));
            return;
        }
        
        // Show loading
        binding.fabSend.hide();
        binding.progressBar.setVisibility(View.VISIBLE);
        
        // Get current user ID for recipient (notes are saved under the user's own ID)
        String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            Toast.makeText(this, R.string.error_authentication_required, Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Use the unified save method for notes
        saveToFirebase(title, content, currentUserId, true);
    }
    
    /**
     * Unified method to save both messages and notes to Firebase
     * @param title Message/note subject
     * @param content Message/note content
     * @param recipientId For messages: recipient's user ID, for notes: can be null
     * @param isNote true if saving a note, false for regular message
     */
    private void saveToFirebase(String title, String content, String recipientId, boolean isNote) {
        String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            Toast.makeText(this, R.string.error_authentication_required, Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        String messageId = databaseHelper.getDatabaseReference().child("messages").push().getKey();
        if (messageId == null) {
            showLoading(false);
            Toast.makeText(this, R.string.error_generic, Toast.LENGTH_SHORT).show();
            return;
        }

        // Unified data structure
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("id", messageId);
        messageData.put("senderId", currentUserId);
        messageData.put("recipientId", isNote ? currentUserId : recipientId);
        messageData.put("content", content);
        messageData.put("subject", title != null ? title : "");
        messageData.put("timestamp", System.currentTimeMillis());
        messageData.put("read", isNote); // Notes are always read
        messageData.put("isNote", isNote);

        // Create updates map
        Map<String, Object> updates = new HashMap<>();
        
        // Always save to /messages
        updates.put("/messages/" + messageId, messageData);
        
        if (isNote) {
            // For notes: save to user's notes
            updates.put("/user-messages/" + currentUserId + "/notes/" + messageId, true);
        } else {
            // For messages: save to sender's sent and recipient's received
            updates.put("/user-messages/" + currentUserId + "/sent/" + messageId, true);
            updates.put("/user-messages/" + recipientId + "/received/" + messageId, true);
        }

        Log.d(TAG, "Saving " + (isNote ? "note" : "message") + " with data: " + updates);

        // Debug log the updates map
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            Log.d("FirebaseUpdate", "Path: " + entry.getKey() + ", Value: " + entry.getValue());
        }

        // Execute updates
        databaseHelper.getDatabaseReference().updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(this,
                            isNote ? R.string.note_saved : R.string.message_sent,
                            Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    
                    // Log basic error info
                    Log.e("FirebaseError", "Error saving " + (isNote ? "note" : "message") + ": " + e.toString());
                    Log.e("FirebaseError", "Error class: " + e.getClass().getName());
                    
                    // Log the full stack trace
                    Log.e("FirebaseError", "Stack trace:");
                    for (StackTraceElement element : e.getStackTrace()) {
                        Log.e("FirebaseError", "    at " + element.toString());
                    }
                    
                    // Log cause if available
                    if (e.getCause() != null) {
                        Log.e("FirebaseError", "Caused by: " + e.getCause().toString());
                        for (StackTraceElement element : e.getCause().getStackTrace()) {
                            Log.e("FirebaseError", "    at " + element.toString());
                        }
                    }
                    
                    // Log the updates map that caused the error
                    Log.e("FirebaseError", "Attempted updates:");
                    for (Map.Entry<String, Object> entry : updates.entrySet()) {
                        Log.e("FirebaseError", "  " + entry.getKey() + " = " + entry.getValue());
                    }
                    
                    Toast.makeText(this,
                            isNote ? R.string.error_saving_note : R.string.error_sending_message,
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void sendMessage() {
        // If we're in note mode, just save the note and return
        if (isNoteMode) {
            saveNote();
            return;
        }
        
        // For regular messages, proceed with sending
        Log.d("SendMessage", "Starting sendMessage");
        String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        
        if (currentUserId == null) {
            Toast.makeText(this, R.string.error_authentication_required, Toast.LENGTH_SHORT).show();
            return;
        }

        // Get input values
        String recipientEmail = binding.inputRecipient.getText().toString().trim();
        String subject = binding.inputSubject.getText().toString().trim();
        String messageText = binding.inputMessage.getText().toString().trim();
        
        // Validate input
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
        
        // Ensure subject is not null for messages
        if (subject == null) {
            subject = "";
        }
        
        // For regular messages, validate recipient
        if (TextUtils.isEmpty(recipientEmail)) {
            binding.inputRecipient.setError("Recipient is required");
            binding.inputRecipient.requestFocus();
            return;
        }
        
        // Check if recipient exists in contacts
        Contact recipientContact = contactsManager.getContactByEmail(recipientEmail);
        if (recipientContact == null) {
            // Create new contact
            contactsManager.addContact(recipientEmail, recipientEmail, recipientEmail);
        }

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

        // Get current user email
        String currentUserEmail = mAuth.getCurrentUser() != null ? 
                mAuth.getCurrentUser().getEmail() : "Unknown";

        if (currentUserId == null) {
            Toast.makeText(this, R.string.error_authentication_required, Toast.LENGTH_SHORT).show();
            return;
        }

        // Process subject before the database query
        final String finalSubject = subject != null ? subject : "";
        
        // Show loading state
        showLoading(true);

        // First, look up the recipient's UID by email
        DatabaseReference usersRef = databaseHelper.getDatabaseReference().child("users");
        Log.d("SendMessage", "Querying users at: " + usersRef.toString());
        
//        usersRef.orderByChild("email")
//                .equalTo(recipientEmail)
//                .addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                        Log.d("SendMessage", "onDataChange, exists: " + dataSnapshot.exists());
//                        if (!dataSnapshot.exists()) {
//                            // No user found with that email
//                            showLoading(false);
//                            binding.inputRecipient.setError("No user found with this email");
//                            Log.d("SendMessage", "No user found with email: " + recipientEmail);
//                            return;
//                        }
//
//                        // Get the first matching user (should be only one)
//                        DataSnapshot userSnapshot = dataSnapshot.getChildren().iterator().next();
//                        String recipientId = userSnapshot.getKey();
//
//                        if (recipientId == null) {
//                            handleError("Invalid recipient");
//                            return;
//                        }
//
//                        // Use the unified save method for messages
//                        saveToFirebase(finalSubject, messageText, recipientId, false);
//                    }
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError databaseError) {
//                        handleError("Error: " + databaseError.getMessage());
//                    }
//
//                    private void handleError(String error) {
//                        runOnUiThread(() -> {
//                            showLoading(false);
//                            Toast.makeText(ComposeMessageActivity.this,
//                                    error,
//                                    Toast.LENGTH_LONG).show();
//                        });
//                        Log.e("ComposeMessage", error);
//                    }
//                });
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
            Toast.makeText(this, isNoteMode ? "Saving note..." : "Sending message...", Toast.LENGTH_SHORT).show();
        } else {
            // Re-enable UI elements
            binding.inputRecipient.setEnabled(true);
            binding.inputSubject.setEnabled(true);
            binding.inputMessage.setEnabled(true);
            binding.checkboxSetReminder.setEnabled(true);
            binding.buttonSetReminder.setEnabled(true);
        }
    }

    private void saveNote(String title, String content) {
        String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        
        if (currentUserId == null) {
            Toast.makeText(this, R.string.error_authentication_required, Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show loading
        showLoading(true);
        
        // Use the unified save method for notes
        // For notes, we use the current user's ID as both sender and recipient
        saveToFirebase(title, content, currentUserId, true);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
