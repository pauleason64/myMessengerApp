package com.example.simplemessenger.ui.messaging;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.simplemessenger.R;
import com.example.simplemessenger.data.model.Contact;
import com.example.simplemessenger.data.ContactsManager;
import com.example.simplemessenger.data.DatabaseHelper;
import com.example.simplemessenger.databinding.ActivityComposeMessageBinding;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class ComposeMessageActivity extends AppCompatActivity {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityComposeMessageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("New Message");

        databaseHelper = DatabaseHelper.getInstance();
        mAuth = FirebaseAuth.getInstance();
        contactsManager = ContactsManager.getInstance();

        setupRecipientAutoComplete();
        loadContacts();
    }

    private void loadContacts() {
        contactsManager.setLoadListener(new ContactsManager.ContactsLoadListener() {
            @Override
            public void onContactsLoaded(List<Contact> contacts) {
                updateAutoCompleteAdapter();
            }

            @Override
            public void onContactAdded(Contact contact) {
                updateAutoCompleteAdapter();
            }

            @Override
            public void onContactRemoved(Contact contact) {
                updateAutoCompleteAdapter();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading contacts: " + error);
            }
        });
    }

    private void setupRecipientAutoComplete() {
        // Set up the AutoCompleteTextView with the contact emails
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this, 
            android.R.layout.simple_dropdown_item_1line, 
            contactEmails
        );
        
        AutoCompleteTextView recipientView = binding.inputRecipient;
        recipientView.setAdapter(adapter);
        
        // Show/hide recipient field based on note mode
        updateUiForMode();
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
                Log.d(TAG, "Contact added successfully: " + contact.getEmailAddress());
                runOnUiThread(() -> {
                    showLoading(false);
                    // Now send the message with the new contact
                    sendMessageToRecipient(contact.getEmailAddress(), finalSubject, messageText, contact.getContactId());
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
        updateNoteToggleMenuItem(menu.findItem(R.id.action_toggle_note));
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_send) {
            sendMessage();
            return true;
        } else if (id == R.id.action_toggle_note) {
            isNoteMode = !isNoteMode;
            updateUiForMode();
            updateNoteToggleMenuItem(item);
            return true;
        } else if (id == android.R.id.home) {
            onBackPressed();
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
            contactsManager.setLoadListener(null);
        }
    }
}
