package com.example.simplemessenger.ui.messaging;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.example.simplemessenger.R;
import com.example.simplemessenger.data.model.Message;
import com.example.simplemessenger.databinding.ActivityMessageDetailBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MessageDetailUiHelper {
    private final ActivityMessageDetailBinding binding;
    private final MessageDetailActivity activity;

    public MessageDetailUiHelper(MessageDetailActivity activity, ActivityMessageDetailBinding binding) {
        this.activity = activity;
        this.binding = binding;
    }

    public void updateUI(Message message) {
        if (message == null || binding == null) {
            Log.e("MessageDetail", "Message or binding is null in updateUI");
            return;
        }

        activity.runOnUiThread(() -> {
            try {
                // Hide progress bar and show content
                binding.progressBar.setVisibility(View.GONE);
                binding.nestedScrollView.setVisibility(View.VISIBLE);

                // Update toolbar title
                if (activity.getSupportActionBar() != null) {
                    activity.getSupportActionBar().setTitle(message.getSubject());
                }
                
                // Set subject and message content
                binding.textSubject.setText(message.getSubject());
                binding.textMessage.setText(message.getContent());
                
                // Handle From/To fields visibility based on whether it's a note
                boolean isNote = message.isNote();
                Log.d("MessageDetail", "updateUI - isNote: " + isNote + 
                                      ", Sender: " + message.getSenderId() + 
                                      ", Recipient: " + message.getRecipientId());
                
                if (isNote) {
                    // Hide From/To fields for notes
                    binding.textFrom.setVisibility(View.GONE);
                    binding.textTo.setVisibility(View.GONE);
                    Log.d("MessageDetail", "Hiding From/To fields for note");
                } else {
                    // Show From/To fields for regular messages
                    binding.textFrom.setVisibility(View.VISIBLE);
                    binding.textTo.setVisibility(View.VISIBLE);
                    
                    // Add From: and To: labels with fallback to user ID if email is not available
                    String senderDisplay = getDisplayName(message, true);
                    String recipientDisplay = getDisplayName(message, false);
                    
                    // Update the UI
                    binding.textFrom.setText(activity.getString(R.string.label_from, senderDisplay));
                    binding.textTo.setText(activity.getString(R.string.label_to, recipientDisplay));
                }
                
                // Format and set timestamp
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
                binding.textDate.setText(sdf.format(new Date(message.getTimestamp())));
                
                // Handle reminder
                if (message.isHasReminder() && message.getReminderTime() > 0) {
                    SimpleDateFormat reminderFormat = new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault());
                    String reminderText = activity.getString(R.string.reminder_set_for, 
                            reminderFormat.format(new Date(message.getReminderTime())));
                    binding.layoutReminder.setVisibility(View.VISIBLE);
                    binding.textReminderTime.setText(reminderText);
                } else {
                    binding.layoutReminder.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                // Log the error with stack trace for debugging
                Log.e("MessageDetail", "Error updating UI: " + e.getMessage(), e);

                // Show a user-friendly error message
                if (binding != null) {
                    binding.textMessage.setText("Error loading message details. Please try again.");
                    binding.textMessage.setTextColor(activity.getResources().getColor(android.R.color.holo_red_dark));

                    // Ensure progress is hidden in case of error
                    binding.progressBar.setVisibility(View.GONE);
                    binding.nestedScrollView.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private String getDisplayName(Message message, boolean isSender) {
        String email = isSender ? message.getSenderEmail() : message.getRecipientEmail();
        String userId = isSender ? message.getSenderId() : message.getRecipientId();
        
        // If we have the current user's Firebase instance, check if this is the current user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // If this is the current user, show "Me" instead of the email
            if (currentUser.getUid().equals(userId)) {
                return "Me";
            }
            // If the email matches the current user's email, show "Me"
            if (currentUser.getEmail() != null && currentUser.getEmail().equals(email)) {
                return "Me";
            }
        }
        
        // If we have an email, use it
        if (email != null && !email.isEmpty()) {
            return email;
        } 
        // If we have a user ID but no email, show a shortened version of the ID
        else if (userId != null) {
            return "User " + userId.substring(0, Math.min(6, userId.length()));
        } 
        // Fallback to generic text
        else {
            return isSender ? "Unknown Sender" : "Unknown Recipient";
        }
    }

    public void showError(String message) {
        if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
            activity.runOnUiThread(() -> 
                Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
            );
        }
    }
}
