package com.example.simplemessenger.ui.adapters;

import android.annotation.SuppressLint;
import android.icu.text.SimpleDateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.simplemessenger.R;
import com.example.simplemessenger.data.ContactsManager;
import com.example.simplemessenger.data.model.Contact;
import com.example.simplemessenger.data.model.Message;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private static final String TAG = "MessageAdapter";
    private List<Message> messages = new ArrayList<>();
    private final Set<String> selectedMessages = new HashSet<>();
    private boolean isMultiSelectMode = false;
    private final OnMessageActionListener actionListener;
    private final boolean isInbox;
    private final ContactsManager contactsManager;

    public interface OnMessageActionListener {
        void onMessageSelected(Message message);
        void onMessageLongClicked(Message message);
        void onSelectionChanged(int selectedCount);
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
        boolean isSelected = selectedMessages.contains(message.getId());
        holder.bind(message, isInbox, isSelected, isMultiSelectMode);
        
        // Set click listener for the item
        holder.itemView.setOnClickListener(v -> {
            if (isMultiSelectMode) {
                toggleSelection(message.getId());
            } else if (actionListener != null) {
                actionListener.onMessageSelected(message);
            }
        });
        
        // Set long click listener for item selection
        holder.itemView.setOnLongClickListener(v -> {
            if (!isMultiSelectMode && actionListener != null) {
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
    public void setMultiSelectMode(boolean enabled) {
        isMultiSelectMode = enabled;
        if (!enabled) {
            selectedMessages.clear();
            if (actionListener != null) {
                actionListener.onSelectionChanged(0);
            }
        }
        notifyDataSetChanged();
    }
    
    public void selectAll() {
        selectedMessages.clear();
        for (Message message : messages) {
            selectedMessages.add(message.getId());
        }
        if (actionListener != null) {
            actionListener.onSelectionChanged(selectedMessages.size());
        }
        notifyDataSetChanged();
    }
    
    public void clearSelections() {
        selectedMessages.clear();
        if (actionListener != null) {
            actionListener.onSelectionChanged(0);
        }
        notifyDataSetChanged();
    }
    
    public List<String> getSelectedMessageIds() {
        return new ArrayList<>(selectedMessages);
    }
    
    public int getSelectedCount() {
        return selectedMessages.size();
    }
    
    public boolean isMultiSelectMode() {
        return isMultiSelectMode;
    }
    
    private int getPositionForId(String messageId) {
        if (messageId == null) return -1;
        for (int i = 0; i < messages.size(); i++) {
            if (messageId.equals(messages.get(i).getId())) {
                return i;
            }
        }
        return -1;
    }
    
    public int toggleSelection(String messageId) {
        if (messageId == null) return 0;
        
        if (selectedMessages.contains(messageId)) {
            selectedMessages.remove(messageId);
        } else {
            selectedMessages.add(messageId);
        }
        
        int position = getPositionForId(messageId);
        if (position != -1) {
            notifyItemChanged(position);
        }
        
        int selectedCount = selectedMessages.size();
        if (actionListener != null) {
            actionListener.onSelectionChanged(selectedCount);
        }
        return selectedCount;
    }
    
    class MessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView textSender;
        private final TextView textTime;
        private final TextView textSubject;
        private final TextView textPreview;
        private final View imageReminder;
        private final ImageView imageCheck;
        private String currentUserId;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textSender = itemView.findViewById(R.id.text_sender);
            textTime = itemView.findViewById(R.id.text_time);
            textSubject = itemView.findViewById(R.id.text_subject);
            textPreview = itemView.findViewById(R.id.text_preview);
            imageReminder = itemView.findViewById(R.id.image_reminder);
            imageCheck = itemView.findViewById(R.id.image_check);
            
            // Get current user ID
            currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
                
            // Set up click listeners
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Message message = messages.get(position);
                    if (isMultiSelectMode) {
                        int selectedCount = toggleSelection(message.getId());
                        // Update the action bar immediately
                        if (actionListener != null) {
                            actionListener.onSelectionChanged(selectedCount);
                        }
                    } else if (actionListener != null) {
                        actionListener.onMessageSelected(message);
                    }
                }
            });
            
            itemView.setOnLongClickListener(v -> {
                if (!isMultiSelectMode) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        Message message = messages.get(position);
                        if (actionListener != null) {
                            actionListener.onMessageLongClicked(message);
                            // Select the item on long click
                            toggleSelection(message.getId());
                            return true;
                        }
                    }
                }
                return false;
            });
        }

        public void bind(Message message, boolean isInbox, boolean isSelected, boolean multiSelectMode) {
            // Always show the checkbox, but update its state based on selection
            if (imageCheck != null) {
                // Set the appropriate drawable based on selection state
                imageCheck.setImageResource(isSelected ? 
                    R.drawable.ic_check_circle_filled : 
                    R.drawable.ic_check_circle_outline);
                
                // Update background to show selection state
                int bgColor = isSelected ? 
                    ContextCompat.getColor(itemView.getContext(), R.color.selected_item_background) : 
                    Color.TRANSPARENT;
                itemView.setBackgroundColor(bgColor);
                
                // Set alpha based on multi-select mode to make it more subtle when not in multi-select
                imageCheck.setAlpha(multiSelectMode ? 1.0f : 0.5f);
            }
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
                // Update the message with the current user's email
                if (isInbox) {
                    message.setSenderEmail(userEmail);
                } else {
                    message.setRecipientEmail(userEmail);
                }
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
                        (message.getSenderEmail() != null ? message.getSenderEmail() : 
                         (message.getSenderId() != null ? "User " + message.getSenderId().substring(0, Math.min(6, message.getSenderId().length())) : "Unknown Sender")) :
                        (message.getRecipientEmail() != null ? message.getRecipientEmail() :
                         (message.getRecipientId() != null ? "User " + message.getRecipientId().substring(0, Math.min(6, message.getRecipientId().length())) : "Unknown Recipient"));
                    textSender.setText(tempName);
                }
            } else {
                // Fallback to showing the email if available, otherwise show ID
                String tempName = isInbox ? 
                    (message.getSenderEmail() != null ? message.getSenderEmail() : 
                     (message.getSenderId() != null ? "User " + message.getSenderId().substring(0, Math.min(6, message.getSenderId().length())) : "Unknown Sender")) :
                    (message.getRecipientEmail() != null ? message.getRecipientEmail() :
                     (message.getRecipientId() != null ? "User " + message.getRecipientId().substring(0, Math.min(6, message.getRecipientId().length())) : "Unknown Recipient"));
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
            
            // First, check if we already have the contact in cache
            Contact existingContact = contactsManager.getContactById(contactId);
            if (existingContact != null && existingContact.getEmailAddress() != null && !existingContact.getEmailAddress().isEmpty()) {
                updateContactInfo(existingContact, message, isInbox);
                return;
            }
            
            // Use the ContactsManager's fetchAndCreateContact method to handle the contact creation
            contactsManager.fetchAndCreateContact(contactId, new ContactsManager.ContactsLoadListener() {
                @Override
                public void onContactAdded(Contact contact) {
                    if (contact != null && contact.getEmailAddress() != null && !contact.getEmailAddress().isEmpty()) {
                        // Update the message with the contact's email
                        String email = contact.getEmailAddress();
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

                @Override
                public void onContactsLoaded(List<Contact> contacts) {
                    // Not used in this context
                }

                @Override
                public void onContactRemoved(Contact contact) {
                    // Not used in this context
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Error fetching contact: " + error);
                }
            });
        }
        
        private void updateContactInfo(Contact contact, Message message, boolean isInbox) {
            if (contact == null || contact.getEmailAddress() == null) {
                return;
            }
            
            String email = contact.getEmailAddress();
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
