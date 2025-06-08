package com.example.simplemessenger.ui.messaging;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.simplemessenger.R;
import com.example.simplemessenger.data.model.Contact;
import com.example.simplemessenger.data.ContactsManager;
import com.example.simplemessenger.data.DatabaseHelper;
import com.example.simplemessenger.data.model.Message;
import com.example.simplemessenger.databinding.ActivityComposeMessageBinding;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComposeMessageActivity extends AppCompatActivity implements ContactsManager.ContactsLoadListener {
    private static final String TAG = "ComposeMessage";
    
    // Intent extras
    public static final String EXTRA_MESSAGE_ID = "message_id";
    public static final String EXTRA_FORWARD_MESSAGE_ID = "forward_message_id";
    public static final String EXTRA_PREVIOUS_MESSAGE_ID = "previous_message_id";
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
    private MenuItem sendMenuItem;
    private String pendingMessageContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityComposeMessageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Initialize Firebase and database
        mAuth = FirebaseAuth.getInstance();
        databaseHelper = DatabaseHelper.getInstance();
        
        // Initialize ContactsManager
        contactsManager = ContactsManager.getInstance();
        contactsManager.setLoadListener(this);
        
        // Set up the action bar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("New Message");
        }
        
        // The send button is in the toolbar and will be handled in onOptionsItemSelected
        
        // Set up recipient autocomplete
        setupRecipientAutocomplete();
        
        loadContacts();
    }

    private void loadContacts() {
        // We'll use the activity as the listener
        contactsManager.setLoadListener(this);
    }
    
    @Override
    public void onContactsLoaded(List<Contact> contacts) {
        updateAutoCompleteAdapter();
    }

    @Override
    public void onContactAdded(Contact contact) {
        if (pendingMessageContent != null && contact != null) {
            // Use a handler to ensure the contact is fully processed
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                sendMessage(contact, pendingMessageContent);
                pendingMessageContent = null; // Clear the pending message
            }, 500); // Small delay to ensure contact is fully processed
        }
        updateAutoCompleteAdapter();
    }

    @Override
    public void onContactRemoved(Contact contact) {
        updateAutoCompleteAdapter();
    }

    @Override
    public void onError(String error) {
        Log.e(TAG, "Error: " + error);
        runOnUiThread(() -> Toast.makeText(this, error, Toast.LENGTH_SHORT).show());
    }

    private void setupRecipientAutocomplete() {
        // Create a list to hold contact display strings (email + name if available)
        List<String> contactDisplayList = new ArrayList<>();
        
        // Create a map to store email to contact ID mapping
        final Map<String, String> emailToContactIdMap = new HashMap<>();
        
        // Populate the list with contact information
        for (Contact contact : contactsManager.getContactsCache().values()) {
            String email = contact.getEmailAddress();
            if (email != null && !email.isEmpty()) {
                String displayName = contact.getDisplayName();
                String displayText = displayName != null && !displayName.isEmpty() ? 
                    String.format("%s <%s>", displayName, email) : email;
                
                contactDisplayList.add(displayText);
                emailToContactIdMap.put(email, contact.getContactId());
            }
        }
        
        // Create and set the adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, contactDisplayList);
        
        // Set up the AutoCompleteTextView
        binding.inputRecipient.setAdapter(adapter);
        binding.inputRecipient.setThreshold(1); // Start showing suggestions after 1 character
        
        // Handle item selection
        binding.inputRecipient.setOnItemClickListener((parent, view, position, id) -> {
            String selectedItem = (String) parent.getItemAtPosition(position);
            // Extract email from the selected item (format: "Display Name <email@example.com>" or "email@example.com")
            String email = selectedItem;
            if (selectedItem.contains("<")) {
                email = selectedItem.substring(selectedItem.indexOf('<') + 1, selectedItem.indexOf('>'));
            }
            
            // Update the field with just the email
            binding.inputRecipient.setText(email);
            binding.inputRecipient.setSelection(email.length());
        });
    }
    
    private void updateUiForMode() {
        if (isNoteMode) {
            binding.layoutRecipient.setVisibility(View.GONE);
            getSupportActionBar().setTitle(R.string.new_note);
        } else {
            binding.layoutRecipient.setVisibility(View.VISIBLE);
            getSupportActionBar().setTitle(R.string.new_message);
        }
    }
    
    private void updateAutoCompleteAdapter() {
        contactEmails.clear();
        for (Contact contact : contactsManager.getCachedContacts()) {
            contactEmails.add(contact.getEmailAddress());
        }
        
        // Notify the adapter that the data has changed
        AutoCompleteTextView recipientView = binding.inputRecipient;
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) recipientView.getAdapter();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
    
    private void sendMessage() {
        String recipientEmail = binding.inputRecipient.getText().toString().trim();
        String subject = binding.inputSubject.getText().toString().trim();
        String messageText = binding.inputMessage.getText().toString().trim();
        
        // Basic validation
        if (!isNoteMode && recipientEmail.isEmpty()) {
            binding.inputRecipient.setError("Recipient email is required");
            return;
        }
        
        if (messageText.isEmpty()) {
            binding.inputMessage.setError("Message is required");
            return;
        }
        
        // If in note mode, save as note and return
        if (isNoteMode) {
            saveNote(subject, messageText);
            return;
        }
        
        // For messages, validate email format
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(recipientEmail).matches()) {
            binding.inputRecipient.setError("Please enter a valid email address");
            return;
        }
        
        // If subject is empty, set a default
        if (subject.isEmpty()) {
            subject = "(No subject)";
        }
        
        final String finalSubject = subject;
        
        // Show loading state
        showLoading(true);
        
        // First, check if recipient is in our contacts
        Contact existingContact = contactsManager.getContactByEmail(recipientEmail);
        if (existingContact != null) {
            // If contact exists, send the message directly
            Log.d(TAG, "Found existing contact in cache: " + recipientEmail);
            sendMessageToRecipient(recipientEmail, finalSubject, messageText, existingContact.getContactId());
            return;
        }
        
        // If not in contacts, use ContactsManager to look up the user
        Log.d(TAG, "Looking up user in ContactsManager: " + recipientEmail);
        contactsManager.fetchAndCreateContact(recipientEmail, new ContactsManager.ContactsLoadListener() {
            @Override
            public void onContactsLoaded(List<Contact> contacts) {
                // Not used for single contact lookup
            }

            @Override
            public void onContactAdded(Contact contact) {
                Log.d(TAG, "onContactAdded called with contact: " + (contact != null ? 
                    "ID: " + contact.getContactId() + ", Email: " + contact.getEmailAddress() : "null"));
                
                if (contact == null) {
                    Log.e(TAG, "Contact is null in onContactAdded");
                    runOnUiThread(() -> {
                        Toast.makeText(ComposeMessageActivity.this, "Error: Contact is null", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_CANCELED);
                        finish();
                    });
                    return;
                }
                
                // Update the local contacts cache immediately with the temporary contact
                if (!contactsManager.getContactsCache().containsKey(contact.getContactId())) {
                    contactsManager.getContactsCache().put(contact.getContactId(), contact);
                    Log.d(TAG, "Added temporary contact to local cache: " + contact.getEmailAddress());
                }
                
                Log.d(TAG, "Processing contact - ID: " + contact.getContactId() + ", Email: " + contact.getEmailAddress());
                
                // Update the recipient field with the contact's email
                runOnUiThread(() -> {
                    try {
                        // Set the recipient field with the contact's email
                        binding.inputRecipient.setText(contact.getEmailAddress());
                        
                        // If we have a pending message, send it now
                        if (!TextUtils.isEmpty(pendingMessageContent)) {
                            Log.d(TAG, "Sending pending message to: " + contact.getEmailAddress());
                            sendMessage(contact, pendingMessageContent);
                            pendingMessageContent = null; // Clear the pending message
                            
                            // Close the activity after a short delay to ensure message is sent
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                setResult(RESULT_OK);
                                finish();
                            }, 300);
                        } else {
                            Log.d(TAG, "No pending message to send");
                            // If no pending message, just close the activity
                            setResult(RESULT_OK);
                            finish();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error in onContactAdded UI update: " + e.getMessage(), e);
                        Toast.makeText(ComposeMessageActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                });
            }

            @Override
            public void onContactRemoved(Contact contact) {
                // Not used here
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error looking up contact: " + error);
                runOnUiThread(() -> {
                    showLoading(false);
                    binding.inputRecipient.setError(error);
                });
            }
        });
    }
    
    private void sendMessage(Contact contact, String messageContent) {
        String subject = binding.inputSubject.getText() != null ? 
            binding.inputSubject.getText().toString().trim() : "";
            
        Log.d(TAG, "sendMessage called - Contact: " + (contact != null ? 
            "ID: " + contact.getContactId() + ", Email: " + contact.getEmailAddress() : "null") + 
            ", Subject: " + subject + ", Message: " + messageContent);
            
        if (contact == null || TextUtils.isEmpty(messageContent)) {
            Log.e(TAG, "Cannot send message - contact or message content is null");
            runOnUiThread(() -> {
                Toast.makeText(this, "Cannot send message: Invalid contact or message", 
                    Toast.LENGTH_SHORT).show();
            });
            return;
        }
        
        // Show loading indicator
        showLoading(true);
        
        // Create a new message with all required fields
        Message message = new Message();
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String currentUserEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        
        message.setId(databaseHelper.getDatabaseReference().child("messages").push().getKey());
        message.setSenderId(currentUserId);
        message.setSenderEmail(currentUserEmail != null ? currentUserEmail : "");
        
        // Use the contact's user ID, not their email
        if (contact.getUserId() != null && !contact.getUserId().isEmpty()) {
            message.setRecipientId(contact.getUserId());
        } else {
            message.setRecipientId(contact.getContactId());
        }
        
        message.setRecipientEmail(contact.getEmailAddress());
        message.setContent(messageContent);
        message.setTimestamp(System.currentTimeMillis());
        message.setRead(false);
        message.setSubject(subject);
        message.setIsNote(false);
        message.setHasReminder(false);
        message.setArchived(false);
        
        // Use DatabaseHelper to send the message
        databaseHelper.sendMessage(message, new DatabaseHelper.DatabaseCallback() {
            @Override
            public void onSuccess(Object result) {
                Log.d(TAG, "Message sent successfully to " + contact.getEmailAddress() + 
                    " (ID: " + contact.getContactId() + ")");
                runOnUiThread(() -> {
                    showLoading(false);
                    // Don't finish here, let onContactAdded handle it
                    // This allows the contact to be properly saved in the background
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to send message: " + error);
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(ComposeMessageActivity.this, "Failed to send message: " + error, 
                        Toast.LENGTH_SHORT).show();
                    setResult(RESULT_CANCELED);
                    finish();
                });
            }
        });
    }
    
    private void sendMessageToRecipient(String recipientEmail, String subject, String message, String recipientId) {
        // Implementation of sendMessageToRecipient
        Log.d(TAG, "Sending message to " + recipientEmail + " (" + recipientId + "): " + subject);
        
        // Here you would implement the actual message sending logic
        // For now, we'll just show a success message
        runOnUiThread(() -> {
            showLoading(false);
            Toast.makeText(this, "Message sent to " + recipientEmail, Toast.LENGTH_SHORT).show();
            finish();
        });
    }
    
    private void saveNote(String title, String content) {
        // Implementation of saveNote
        showLoading(true);
        
        // Here you would implement the actual note saving logic
        // For now, we'll just show a success message
        runOnUiThread(() -> {
            showLoading(false);
            Toast.makeText(this, "Note saved", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
    
    private void showLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (sendMenuItem != null) {
            sendMenuItem.setEnabled(!isLoading);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_compose, menu);
        sendMenuItem = menu.findItem(R.id.action_send);
        MenuItem noteToggleItem = menu.findItem(R.id.action_toggle_note);
        if (noteToggleItem != null) {
            updateNoteToggleMenuItem(noteToggleItem);
        }
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_send) {
            String recipient = binding.inputRecipient.getText().toString().trim();
            String message = binding.inputMessage.getText().toString().trim();
            
            if (TextUtils.isEmpty(recipient)) {
                Toast.makeText(this, "Please enter a recipient", Toast.LENGTH_SHORT).show();
                return true;
            }
            
            if (TextUtils.isEmpty(message)) {
                Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
                return true;
            }
            
            // Check if we already have this contact locally
            Contact existingContact = null;
            for (Contact contact : contactsManager.getContactsCache().values()) {
                if (recipient.equalsIgnoreCase(contact.getEmailAddress())) {
                    existingContact = contact;
                    break;
                }
            }
            
            if (existingContact != null) {
                // We have the contact, send the message directly
                Log.d(TAG, "Sending message to existing contact: " + existingContact.getEmailAddress());
                sendMessage(existingContact, message);
            } else {
                // We need to create the contact first
                Log.d(TAG, "Creating new contact for: " + recipient);
                pendingMessageContent = message;
                contactsManager.fetchAndCreateContact(recipient, this);
            }
            return true;
        } else if (id == R.id.action_toggle_note) {
            isNoteMode = !isNoteMode;
            updateNoteToggleMenuItem(item);
            updateUiForMode();
            return true;
        } else if (id == android.R.id.home) {
            finish();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    private void updateNoteToggleMenuItem(MenuItem menuItem) {
        if (menuItem != null) {
            menuItem.setIcon(isNoteMode ? 
                R.drawable.ic_email : 
                R.drawable.ic_note);
            menuItem.setTitle(isNoteMode ? 
                R.string.action_switch_to_message : 
                R.string.action_switch_to_note);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (contactsManager != null) {
            // Clear the listener to prevent memory leaks
            contactsManager.clearPreviousListener();
            contactsManager.cleanupContacts();
        }
    }
}
