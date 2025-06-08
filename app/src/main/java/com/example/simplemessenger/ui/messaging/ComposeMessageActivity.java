package com.example.simplemessenger.ui.messaging;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.simplemessenger.R;
import com.example.simplemessenger.data.model.Contact;
import com.example.simplemessenger.data.ContactsManager;
import com.example.simplemessenger.data.DatabaseHelper;
import com.example.simplemessenger.data.model.Message;
import com.example.simplemessenger.databinding.ActivityComposeMessageBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

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
        }
        
        // Note: Bottom navigation and FAB have been removed as they're not needed
        // The send functionality is handled by the send button in the toolbar
        
        // Check if we're in note mode from intent
        if (getIntent() != null) {
            isNoteMode = getIntent().getBooleanExtra(EXTRA_IS_NOTE, false) || 
                        getIntent().getBooleanExtra(EXTRA_NOTE_MODE, false);
        }
        
        // Set appropriate title based on mode
        updateUiForMode();
        
        // Set up recipient autocomplete
        setupRecipientAutocomplete();
        
        // Load contacts
        loadContacts();
        
        // Request focus and show keyboard for the appropriate field
        binding.getRoot().postDelayed(() -> {
            if (isNoteMode) {
                if (binding.inputSubject.requestFocus()) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(binding.inputSubject, InputMethodManager.SHOW_IMPLICIT);
                }
            } else {
                if (binding.inputRecipient.requestFocus()) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(binding.inputRecipient, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        }, 100);
    }

    private void loadContacts() {
        // Set this activity as the load listener
        contactsManager.setLoadListener(this);
        
        // Load contacts if not already loaded
        if (contactsManager.getCachedContacts().isEmpty()) {
            // Show loading indicator
            showLoading(true);
            
            // Initialize contacts - this will trigger onContactsLoaded when done
            try {
                contactsManager.initializeContacts();
            } catch (Exception e) {
                Log.e(TAG, "Error initializing contacts", e);
                showLoading(false);
                Toast.makeText(this, "Error loading contacts: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
        } else {
            // Contacts already loaded, update the adapter
            updateAutoCompleteAdapter();
        }
    }
    
    @Override
    public void onContactsLoaded(List<Contact> contacts) {
        runOnUiThread(() -> {
            showLoading(false);
            updateAutoCompleteAdapter();
        });
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
        // Create a simple adapter that shows both name and email
        ArrayAdapter<String> emailAdapter = new ArrayAdapter<String>(
            this, 
            android.R.layout.simple_dropdown_item_1line, 
            new ArrayList<>()
        ) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                String email = getItem(position);
                if (email != null) {
                    // Try to find the contact to show name if available
                    Contact contact = contactsManager.getContactByEmail(email);
                    if (contact != null && contact.getDisplayName() != null && !contact.getDisplayName().isEmpty()) {
                        ((TextView) view).setText(String.format("%s <%s>", contact.getDisplayName(), email));
                    } else {
                        ((TextView) view).setText(email);
                    }
                }
                return view;
            }
        };
        
        // Set up the AutoCompleteTextView
        binding.inputRecipient.setAdapter(emailAdapter);
        binding.inputRecipient.setThreshold(1); // Start showing suggestions after 1 character
        
        // Handle item selection
        binding.inputRecipient.setOnItemClickListener((parent, view, position, id) -> {
            // Get the selected email
            String selectedEmail = parent.getItemAtPosition(position).toString();
            
            // Set the text and move cursor to end
            binding.inputRecipient.setText(selectedEmail);
            binding.inputRecipient.setSelection(selectedEmail.length());
            
            // Dismiss the dropdown
            binding.inputRecipient.dismissDropDown();
            
            // Move focus to subject field
            binding.inputSubject.requestFocus();
        });
        
        // Update the adapter when the text changes
        binding.inputRecipient.addTextChangedListener(new TextWatcher() {
            private final Handler handler = new Handler(Looper.getMainLooper());
            private Runnable runnable;
            
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Clear any previous error when user types
                binding.inputRecipient.setError(null);
                
                // Cancel any pending searches
                if (runnable != null) {
                    handler.removeCallbacks(runnable);
                }
                
                // Debounce the search to avoid too many updates
                String searchText = s.toString().trim();
                if (searchText.length() >= 1) {  // Only search if there's at least 1 character
                    runnable = () -> updateEmailSuggestions(searchText);
                    handler.postDelayed(runnable, 300); // 300ms delay
                } else {
                    // Clear the dropdown if search text is too short
                    emailAdapter.clear();
                    emailAdapter.notifyDataSetChanged();
                }
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
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
    
    private void updateEmailSuggestions(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            return;
        }
        
        String searchLower = searchText.toLowerCase();
        List<String> matchingEmails = new ArrayList<>();
        
        // Search through all contacts
        for (Contact contact : contactsManager.getCachedContacts()) {
            String email = contact.getEmailAddress();
            String name = contact.getDisplayName();
            
            if (email != null && email.toLowerCase().contains(searchLower)) {
                matchingEmails.add(email);
            } else if (name != null && name.toLowerCase().contains(searchLower)) {
                matchingEmails.add(email);
            }
        }
        
        // Update the adapter on the UI thread
        runOnUiThread(() -> {
            ArrayAdapter<String> adapter = (ArrayAdapter<String>) binding.inputRecipient.getAdapter();
            if (adapter != null) {
                adapter.clear();
                adapter.addAll(matchingEmails);
                adapter.notifyDataSetChanged();
                
                // Show dropdown if there are results and the field has focus
                if (!matchingEmails.isEmpty() && binding.inputRecipient.hasFocus()) {
                    binding.inputRecipient.showDropDown();
                }
            }
        });
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
            sendMessage(existingContact, messageText);
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
                        showLoading(false);
                        Toast.makeText(ComposeMessageActivity.this, "Error: Contact is null", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
                
                // Update the local contacts cache immediately with the temporary contact
                if (!contactsManager.getContactsCache().containsKey(contact.getContactId())) {
                    contactsManager.getContactsCache().put(contact.getContactId(), contact);
                    Log.d(TAG, "Added temporary contact to local cache: " + contact.getEmailAddress());
                }
                
                Log.d(TAG, "Sending message to contact - ID: " + contact.getContactId() + ", Email: " + contact.getEmailAddress());
                
                // Send the message with the found contact
                runOnUiThread(() -> {
                    try {
                        // Set the recipient field with the contact's email
                        binding.inputRecipient.setText(contact.getEmailAddress());
                        // Send the message
                        sendMessage(contact, messageText);
                    } catch (Exception e) {
                        Log.e(TAG, "Error in onContactAdded: " + e.getMessage(), e);
                        showLoading(false);
                        Toast.makeText(ComposeMessageActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(ComposeMessageActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void sendMessage(Contact contact, String messageContent) {
        String subject = binding.inputSubject.getText() != null ? 
            binding.inputSubject.getText().toString().trim() : "";
            
        Log.d(TAG, "sendMessage called - Contact: " + (contact != null ? 
            "ID: " + contact.getContactId() + ", UserID: " + contact.getUserId() + 
            ", Email: " + contact.getEmailAddress() : "null") + 
            ", Subject: " + subject + ", Message: " + messageContent);
            
        if (contact == null || TextUtils.isEmpty(messageContent)) {
            String error = "Cannot send message - contact or message content is null";
            Log.e(TAG, error);
            runOnUiThread(() -> {
                showLoading(false);
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            });
            return;
        }
        
        // Validate we have a valid recipient ID
        String recipientId = contact.getUserId() != null && !contact.getUserId().isEmpty() ? 
            contact.getUserId() : contact.getContactId();
            
        if (recipientId == null || recipientId.isEmpty()) {
            String error = "Cannot determine recipient ID for contact: " + contact.getEmailAddress();
            Log.e(TAG, error);
            runOnUiThread(() -> {
                showLoading(false);
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
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
        
        // Always use the contact's user ID as recipient ID
        message.setRecipientId(recipientId);
        message.setRecipientEmail(contact.getEmailAddress());
        
        // Set message content and metadata
        message.setContent(messageContent);
        message.setTimestamp(System.currentTimeMillis());
        message.setRead(false);
        message.setSubject(subject);
        message.setIsNote(false);
        message.setHasReminder(false);
        message.setArchived(false);
        
        Log.d(TAG, "Prepared message - Sender: " + currentUserId + 
            ", Recipient: " + recipientId + ", Email: " + contact.getEmailAddress());
        
        // Use DatabaseHelper to send the message
        databaseHelper.sendMessage(message, new DatabaseHelper.DatabaseCallback() {
            @Override
            public void onSuccess(Object result) {
                Log.d(TAG, "Message sent successfully to " + contact.getEmailAddress() + 
                    " (ID: " + contact.getContactId() + ")");
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(ComposeMessageActivity.this, "Message sent successfully", 
                        Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
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
        showLoading(true);
        
        // Get current user
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            showLoading(false);
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create a new note message
        Message note = new Message();
        String noteId = databaseHelper.getDatabaseReference().child("messages").push().getKey();
        note.setId(noteId);
        note.setSenderId(currentUser.getUid());
        note.setSenderEmail(currentUser.getEmail() != null ? currentUser.getEmail() : "");
        note.setRecipientId(currentUser.getUid()); // Notes are saved to self
        note.setRecipientEmail(currentUser.getEmail() != null ? currentUser.getEmail() : "");
        note.setContent(content);
        note.setSubject(title.isEmpty() ? "(No title)" : title);
        note.setTimestamp(System.currentTimeMillis());
        note.setRead(true); // Notes are marked as read by default
        note.setIsNote(true);
        note.setHasReminder(false);
        note.setArchived(false);
        
        // Save the note using DatabaseHelper
        databaseHelper.sendMessage(note, new DatabaseHelper.DatabaseCallback() {
            @Override
            public void onSuccess(Object result) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(ComposeMessageActivity.this, "Note saved successfully", 
                        Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(ComposeMessageActivity.this, 
                        "Failed to save note: " + error, Toast.LENGTH_LONG).show();
                });
            }
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
            String subject = binding.inputSubject.getText().toString().trim();
            
            // Validate inputs based on mode
            if (!isNoteMode) {
                // Message validation
                if (TextUtils.isEmpty(recipient)) {
                    binding.inputRecipient.setError("Recipient is required");
                    return true;
                }
                
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(recipient).matches()) {
                    binding.inputRecipient.setError("Please enter a valid email");
                    return true;
                }
            }
            
            if (TextUtils.isEmpty(message)) {
                binding.inputMessage.setError(isNoteMode ? 
                    "Note content cannot be empty" : "Message cannot be empty");
                return true;
            }
            
            if (isNoteMode) {
                // Save as note
                saveNote(subject, message);
                return true;
            }
            
            // For messages, check for existing contact
            Contact existingContact = contactsManager.getContactByEmail(recipient);
            
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
