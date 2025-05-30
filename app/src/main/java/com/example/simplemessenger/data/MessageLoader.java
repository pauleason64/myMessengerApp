package com.example.simplemessenger.data;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.simplemessenger.data.model.Contact;
import com.example.simplemessenger.data.model.Message;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MessageLoader {
    private static final String TAG = "MessageLoader";
    
    public interface MessageLoadListener {
        void onMessagesLoaded(List<Message> messages);
        void onError(String error);
    }
    
    private final DatabaseHelper databaseHelper;
    private final ContactsManager contactsManager;
    private final String currentUserId;
    private final boolean isInbox;
    private final String currentSortField;
    private final boolean isAscending;
    
    private ValueEventListener messageListener;
    private Query messagesQuery;
    
    public MessageLoader(DatabaseHelper databaseHelper, 
                        ContactsManager contactsManager,
                        String currentUserId, 
                        boolean isInbox,
                        String currentSortField,
                        boolean isAscending) {
        this.databaseHelper = databaseHelper;
        this.contactsManager = contactsManager;
        this.currentUserId = currentUserId;
        this.isInbox = isInbox;
        this.currentSortField = currentSortField;
        this.isAscending = isAscending;
    }
    
    public void loadMessages(MessageLoadListener listener) {
        Log.d(TAG, "Starting message loading for user: " + currentUserId);
        
        if (currentUserId == null) {
            String error = "Current user ID is null. User not authenticated?";
            Log.e(TAG, error);
            listener.onError(error);
            return;
        }
        
        // Remove any existing listener to prevent duplicates
        removeListener();
        
        // Determine which messages to load based on current tab
        String messageType = isInbox ? "received" : "sent";
        
        // Query messages
        DatabaseReference userMessagesRef = databaseHelper.getDatabaseReference()
                .child("user-messages")
                .child(currentUserId)
                .child(messageType);
                
        // Add a listener to check if the reference exists
        userMessagesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Log.d(TAG, "No " + messageType + " messages found for user");
                    listener.onMessagesLoaded(new ArrayList<>());
                    return;
                }
                
                // If reference exists, proceed with the query
                messagesQuery = userMessagesRef.orderByChild(currentSortField);
                
                if (!isAscending) {
                    messagesQuery = messagesQuery.limitToLast(100); // Only get last 100 messages
                }
                
                // Add a value event listener for real-time updates
                messageListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        Log.d(TAG, "onDataChange, messages count: " + dataSnapshot.getChildrenCount());
                        
                        if (dataSnapshot.getChildrenCount() == 0) {
                            listener.onMessagesLoaded(new ArrayList<>());
                            return;
                        }
                        
                        final List<Message> loadedMessages = new ArrayList<>();
                        final int[] completedFetches = {0};
                        final int totalMessages = (int) dataSnapshot.getChildrenCount();
                        
                        for (DataSnapshot messageRef : dataSnapshot.getChildren()) {
                            String messageId = messageRef.getKey();
                            Log.d(TAG, "Fetching message with ID: " + messageId);
                            
                            databaseHelper.getDatabaseReference()
                                    .child("messages")
                                    .child(messageId)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            Message message = snapshot.getValue(Message.class);
                                            if (message != null) {
                                                // Set message ID
                                                message.setId(messageId);
                                                
                                                // Look up contact info from cache
                                                String contactId = isInbox ? message.getSenderId() : message.getRecipientId();
                                                Contact contact = contactsManager.getContactById(contactId);
                                                
                                                if (contact != null) {
                                                    // If contact is in cache, we don't need to set contact info in the message
                                                    // as the message already contains the necessary sender/recipient info
                                                } else {
                                                    // If contact not in cache, try to fetch it
                                                    contactsManager.fetchAndCreateContact(contactId, new ContactsManager.ContactsLoadListener() {
                                                        @Override
                                                        public void onContactsLoaded(List<Contact> contacts) {}

                                                        @Override
                                                        public void onContactAdded(Contact newContact) {
                                                            // No need to update message with contact info
                                                            // as the message already contains the necessary sender/recipient info
                                                            // Just notify listener about the update
                                                            listener.onMessagesLoaded(loadedMessages);
                                                        }

                                                        @Override
                                                        public void onContactRemoved(Contact contact) {}

                                                        @Override
                                                        public void onError(String error) {
                                                            Log.e(TAG, "Error fetching contact: " + error);
                                                        }
                                                    });
                                                }
                                                
                                                loadedMessages.add(message);
                                                completedFetches[0]++;

                                                if (completedFetches[0] == totalMessages) {
                                                    sortAndNotify(loadedMessages, listener);
                                                }
                                            } else {
                                                completedFetches[0]++;
                                                if (completedFetches[0] == totalMessages) {
                                                    sortAndNotify(loadedMessages, listener);
                                                }
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            Log.e(TAG, "Error loading message: " + error.getMessage());
                                            completedFetches[0]++;
                                            if (completedFetches[0] == totalMessages) {
                                                sortAndNotify(loadedMessages, listener);
                                            }
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "Error loading messages: " + databaseError.getMessage());
                        listener.onError(databaseError.getMessage());
                    }
                };
                
                // Add the listener to the query
                messagesQuery.addValueEventListener(messageListener);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking messages reference: " + error.getMessage());
                listener.onError(error.getMessage());
            }
        });
    }
    
    private void sortAndNotify(List<Message> messages, MessageLoadListener listener) {
        if (currentSortField.equals("timestamp")) {
            if (!isAscending) {
                Collections.reverse(messages);
            }
        } else if (currentSortField.equals("subject")) {
            messages.sort((m1, m2) -> {
                String s1 = m1.getSubject() != null ? m1.getSubject() : "";
                String s2 = m2.getSubject() != null ? m2.getSubject() : "";
                return isAscending ? s1.compareTo(s2) : s2.compareTo(s1);
            });
        }
        listener.onMessagesLoaded(messages);
    }
    
    public void removeListener() {
        if (messageListener != null && messagesQuery != null) {
            messagesQuery.removeEventListener(messageListener);
            messageListener = null;
        }
    }
    
    public void cleanup() {
        removeListener();
    }
}
