package com.example.simplemessenger.ui.messaging;

import android.content.Intent;
import android.os.Bundle;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MessageDetailActivity extends AppCompatActivity {

    private ActivityMessageDetailBinding binding;
    private DatabaseHelper databaseHelper;
    private FirebaseAuth mAuth;
    private String messageId;
    private Message message;

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

        // Initialize Firebase instances
        databaseHelper = DatabaseHelper.getInstance();
        mAuth = FirebaseAuth.getInstance();

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
                    message = dataSnapshot.getValue(Message.class);
                    if (message != null) {
                        message.setId(dataSnapshot.getKey());
                        updateUI();
                        
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
        if (message == null) {
            return;
        }

        // Set sender and recipient
        binding.textFrom.setText(getString(R.string.label_from, message.getSenderEmail()));
        binding.textTo.setText(getString(R.string.label_to, message.getRecipientEmail()));
        
        // Set subject
        binding.textSubject.setText(message.getSubject());
        
        // Set message body
        binding.textMessage.setText(message.getMessage());
        
        // Set date and time
        long timestamp = message.getTimestamp();
        if (timestamp > 0) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault());
            String formattedDate = dateFormat.format(new java.util.Date(timestamp));
            binding.textDate.setText(formattedDate);
        }
        
        // Show reminder info if available
        if (message.isHasReminder()) {
            long reminderTime = message.getReminderTime();
            if (reminderTime > 0) {
                binding.layoutReminder.setVisibility(View.VISIBLE);
                SimpleDateFormat reminderFormat = new SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault());
                String reminderText = getString(R.string.reminder_set_for, 
                        reminderFormat.format(new java.util.Date(reminderTime)));
                binding.textReminderTime.setText(reminderText);
            } else {
                binding.layoutReminder.setVisibility(View.GONE);
            }
        } else {
            binding.layoutReminder.setVisibility(View.GONE);
        }
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
        intent.putExtra("message", message.getMessage());
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

        String shareText = message.getSubject() + "\n\n" + message.getMessage();
        
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
