package com.example.simplemessenger.ui.messaging;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.simplemessenger.R;
import com.example.simplemessenger.data.DatabaseHelper;
import com.example.simplemessenger.data.model.Message;
import com.example.simplemessenger.databinding.ActivityComposeMessageBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class ComposeMessageActivity extends AppCompatActivity {

    private ActivityComposeMessageBinding binding;
    private DatabaseHelper databaseHelper;
    private FirebaseAuth mAuth;
    private DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityComposeMessageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase and DatabaseHelper
        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference();
        databaseHelper = DatabaseHelper.getInstance();

        // Set up the toolbar
        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.title_compose_message);
        }

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

    private void showReminderDialog() {
        // TODO: Implement reminder dialog
        Toast.makeText(this, "Reminder functionality will be implemented here", Toast.LENGTH_SHORT).show();
    }

    private void sendMessage() {
        String recipient = binding.inputRecipient.getText().toString().trim();
        String subject = binding.inputSubject.getText().toString().trim();
        String messageText = binding.inputMessage.getText().toString().trim();

        // Validate inputs
        if (TextUtils.isEmpty(recipient)) {
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
        String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        String currentUserEmail = mAuth.getCurrentUser() != null ? 
                mAuth.getCurrentUser().getEmail() : "Unknown";

        if (currentUserId == null) {
            Toast.makeText(this, R.string.error_authentication_required, Toast.LENGTH_SHORT).show();
            return;
        }

        // Create message object
        String messageId = database.child("messages").push().getKey();
        if (messageId == null) {
            Toast.makeText(this, R.string.error_creating_message, Toast.LENGTH_SHORT).show();
            return;
        }

        long timestamp = System.currentTimeMillis();
        
        // Create message map
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("id", messageId);
        messageMap.put("senderId", currentUserId);
        messageMap.put("senderEmail", currentUserEmail);
        messageMap.put("recipientEmail", recipient);
        messageMap.put("subject", subject);
        messageMap.put("message", messageText);
        messageMap.put("timestamp", timestamp);
        messageMap.put("read", false);
        messageMap.put("hasReminder", binding.checkboxSetReminder.isChecked());
        messageMap.put("archived", false);
        
        // If reminder is set, add reminder time (for future implementation)
        if (binding.checkboxSetReminder.isChecked()) {
            // TODO: Set reminder time
            // messageMap.put("reminderTime", reminderTime);
        }

        // Create updates map for atomic updates
        Map<String, Object> updates = new HashMap<>();
        updates.put("/messages/" + messageId, messageMap);
        
        // Add to user's sent messages
        updates.put("/user-messages/" + currentUserId + "/" + messageId + "/id", messageId);
        updates.put("/user-messages/" + currentUserId + "/" + messageId + "/timestamp", timestamp);
        updates.put("/user-messages/" + currentUserId + "/" + messageId + "/archived", false);
        
        // Execute updates
        database.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ComposeMessageActivity.this,
                            R.string.message_sent, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ComposeMessageActivity.this,
                            e != null ? e.getMessage() : getString(R.string.error_sending_message),
                            Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
