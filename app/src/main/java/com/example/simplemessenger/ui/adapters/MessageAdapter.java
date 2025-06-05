package com.example.simplemessenger.ui.adapters;

import android.annotation.SuppressLint;
import android.icu.text.SimpleDateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.simplemessenger.R;
import com.example.simplemessenger.data.ContactsManager;
import com.example.simplemessenger.data.model.Contact;
import com.example.simplemessenger.data.model.Message;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private static final String TAG = "MessageAdapter";
    private List<Message> messages = new ArrayList<>();
    private final OnMessageActionListener actionListener;
    private final boolean isInbox;
    private final ContactsManager contactsManager;

    public interface OnMessageActionListener {
        void onMessageSelected(Message message);
        void onMessageLongClicked(Message message);
    }

    public MessageAdapter(OnMessageActionListener actionListener, boolean isInbox) {
        this.actionListener = actionListener;
        this.isInbox = isInbox;
        this.contactsManager = ContactsManager.getInstance();
        
        // Initialize contacts if not already done
        if (contactsManager != null) {
            contactsManager.initializeContacts();
        } else {
            Log.e(TAG, "ContactsManager instance is null");
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateMessages(List<Message> newMessages) {
        this.messages.clear();
        if (newMessages != null) {
            this.messages.addAll(newMessages);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.bind(message, isInbox);
        
        // Set click listener for the item
        holder.itemView.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onMessageSelected(message);
            }
        });
        
        // Set long click listener for item selection
        holder.itemView.setOnLongClickListener(v -> {
            if (actionListener != null) {
                actionListener.onMessageLongClicked(message);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    // ViewHolder inner class
    class MessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView textSender;
        private final TextView textTime;
        private final TextView textSubject;
        private final TextView textPreview;
        private final View imageReminder;
        private String currentUserId;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textSender = itemView.findViewById(R.id.text_sender);
            textTime = itemView.findViewById(R.id.text_time);
            textSubject = itemView.findViewById(R.id.text_subject);
            textPreview = itemView.findViewById(R.id.text_preview);
            imageReminder = itemView.findViewById(R.id.image_reminder);
            
            // Get current user ID
            currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        }

        public void bind(Message message, boolean isInbox) {
            // Clear all views first
            textSender.setText("");
            textTime.setText("");
            textSubject.setText("");
            textPreview.setText("");
            imageReminder.setVisibility(View.GONE);
            
            if (message == null) {
                return;
            }
            
            // Always update message info first to ensure all fields are set
            updateMessageInfo(message);
            
            // For notes, show "Note" as the sender
            if (message.getIsNote()) {
                String noteLabel = itemView.getContext().getString(R.string.label_note);
                textSender.setText(noteLabel);
                textSender.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_note_small, 0, 0, 0);
                textSender.setCompoundDrawablePadding(itemView.getContext().getResources()
                        .getDimensionPixelSize(R.dimen.small_padding));
                return;
            }
            
            // For messages, resolve contact information
            String contactId = isInbox ? message.getSenderId() : message.getRecipientId();
            String currentEmail = isInbox ? message.getSenderEmail() : message.getRecipientEmail();
            
            // If we already have the email, use it
            if (currentEmail != null && !currentEmail.isEmpty()) {
                updateDisplayName(message, currentEmail, isInbox);
            } 
            // If it's the current user, use their email
            else if (contactId != null && contactId.equals(currentUserId)) {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                String userEmail = currentUser != null ? 
                    (currentUser.getEmail() != null ? currentUser.getEmail() : "Me") : "Me";
                updateDisplayName(message, userEmail, isInbox);
            }
            // Otherwise, try to get from contacts cache
            else if (contactId != null && contactsManager != null) {
                // First check if we have the contact in cache
                Contact contact = contactsManager.getContactById(contactId);
                if (contact != null && contact.getEmailAddress() != null && !contact.getEmailAddress().isEmpty()) {
                    String email = contact.getEmailAddress();
                    // Update the message with the contact's email
                    if (isInbox) {
                        message.setSenderEmail(email);
                    } else {
                        message.setRecipientEmail(email);
                    }
                    updateDisplayName(message, email, isInbox);
                } else {
                    // If not in cache, fetch it
                    fetchAndUpdateContact(contactId, message, isInbox);
                    
                    // Show a temporary display name while loading
                    String tempName = isInbox ? 
                        (message.getSenderId() != null ? "User " + message.getSenderId().substring(0, Math.min(6, message.getSenderId().length())) : "Unknown Sender") :
                        (message.getRecipientId() != null ? "User " + message.getRecipientId().substring(0, Math.min(6, message.getRecipientId().length())) : "Unknown Recipient");
                    textSender.setText(tempName);
                }
            } else {
                // Fallback to showing the ID
                String tempName = isInbox ? 
                    (message.getSenderId() != null ? "User " + message.getSenderId().substring(0, Math.min(6, message.getSenderId().length())) : "Unknown Sender") :
                    (message.getRecipientId() != null ? "User " + message.getRecipientId().substring(0, Math.min(6, message.getRecipientId().length())) : "Unknown Recipient");
                textSender.setText(tempName);
            }
            
            // Clear any compound drawables for messages (notes already handled above)
            if (!message.getIsNote()) {
                textSender.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
            }
        }
        
        private void fetchAndUpdateContact(String contactId, Message message, boolean isInbox) {
            if (contactsManager == null) {
                Log.e(TAG, "ContactsManager is null, cannot fetch contact");
                return;
            }
            
            contactsManager.fetchAndCreateContact(contactId, new ContactsManager.ContactsLoadListener() {
                @Override
                public void onContactAdded(Contact contact) {
                    if (contact != null && contact.getUserId().equals(contactId)) {
                        String email = contact.getEmailAddress();
                        if (email != null && !email.isEmpty()) {
                            // Update the message with the contact's email
                            if (isInbox) {
                                message.setSenderEmail(email);
                            } else {
                                message.setRecipientEmail(email);
                            }
                            
                            // Update the UI on the main thread
                            if (itemView.getHandler() != null) {
                                itemView.post(() -> {
                                    updateDisplayName(message, email, isInbox);
                                    updateMessageInfo(message);
                                });
                            }
                        }
                    }
                }

                @Override
                public void onContactsLoaded(List<Contact> contacts) {}

                @Override
                public void onContactRemoved(Contact contact) {}

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Error fetching contact: " + error);
                }
            });
        }
        
        private void updateDisplayName(Message message, String email, boolean isInbox) {
            // This method is called when we have the email to display
            if (email != null && !email.isEmpty()) {
                textSender.setText(email);
                
                // If we're in the outbox and this is the current user's email, show "Me"
                if (!isInbox && currentUserId != null) {
                    String currentUserEmail = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                        FirebaseAuth.getInstance().getCurrentUser().getEmail() : null;
                    if (email.equals(currentUserEmail)) {
                        textSender.setText("Me");
                    }
                }
            }
        }
        
        private void updateMessageInfo(Message message) {
            // Format and set time
            long timestamp = message.getTimestamp();
            String timeText = "";
            if (timestamp > 0) {
                // Format the timestamp to a readable date/time
                SimpleDateFormat sdf = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());
                timeText = sdf.format(new Date(timestamp));
            }
            textTime.setText(timeText);
            
            // Set subject and preview
            textSubject.setText(message.getSubject() != null ? message.getSubject() : "(No subject)");
            textPreview.setText(message.getContent() != null ? message.getContent() : "");
            
            // Show reminder icon if there's a reminder set
            imageReminder.setVisibility(message.isHasReminder() ? View.VISIBLE : View.GONE);
        }
    }
}
