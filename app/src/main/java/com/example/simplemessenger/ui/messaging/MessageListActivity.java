package com.example.simplemessenger.ui.messaging;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.simplemessenger.R;
import com.example.simplemessenger.data.ContactsManager;
import com.example.simplemessenger.data.DatabaseHelper;
import com.example.simplemessenger.data.model.Contact;
import com.example.simplemessenger.data.model.Message;
import com.example.simplemessenger.databinding.ActivityMessageListBinding;
import com.example.simplemessenger.ui.messaging.adapter.MessageAdapter;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MessageListActivity extends AppCompatActivity {

    private ActivityMessageListBinding binding;
    private DatabaseHelper databaseHelper;
    private FirebaseAuth mAuth;
    private ActionMode actionMode;
    private String currentUserId;
    private final List<Message> messages = new ArrayList<>();
    private ValueEventListener messageListener;
    private Query messagesQuery;
    private boolean isAscending = true;
    private boolean isInbox = true;
    private String currentSortField = "timestamp";
    private ContactsManager contactsManager;

    private final MessageAdapter adapter = new MessageAdapter(new MessageAdapter.OnMessageActionListener() {
        @Override
        public void onMessageSelected(Message message) {
            // Open message detail
            Intent intent = new Intent(MessageListActivity.this, MessageDetailActivity.class);
            intent.putExtra("message_id", message.getId());
            startActivity(intent);
        }

        @Override
        public void onMessageLongClicked(Message message) {
            if (actionMode == null) {
                actionMode = startSupportActionMode(actionModeCallback);
            }
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMessageListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase instances
        databaseHelper = DatabaseHelper.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        contactsManager = ContactsManager.getInstance();

        // Set up toolbar and its components
        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.title_messages);
        }

        // Set up inbox/outbox toggle
        binding.radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            isInbox = checkedId == R.id.radio_inbox;
            updateTitle();
            loadMessages();
        });

        // Set up header click listeners
        binding.textDateHeader.setOnClickListener(v -> {
            currentSortField = "timestamp";
            isAscending = !isAscending;
            updateTitle();
            loadMessages();
        });

        binding.textSubjectHeader.setOnClickListener(v -> {
            currentSortField = "subject";
            isAscending = !isAscending;
            updateTitle();
            loadMessages();
        });
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.title_messages);
        }

        // Set up RecyclerView
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.addItemDecoration(
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        // Set up swipe to refresh
        binding.swipeRefreshLayout.setOnRefreshListener(this::loadMessages);

        // Set up title text
        updateTitle();

        // Set up FAB
        binding.fab.setOnClickListener(view -> {
            startActivity(new Intent(this, ComposeMessageActivity.class));
        });

        // Set up adapter
        binding.recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadContactsAndMessages();
    }

    private void loadContactsAndMessages() {
        // First load contacts
        contactsManager.initializeContacts();
        contactsManager.setLoadListener(new ContactsManager.ContactsLoadListener() {
            @Override
            public void onContactsLoaded(List<Contact> contacts) {
                if (contacts != null) {
                Log.d("MessageList", "Contacts loaded: " + contacts.size() + " contacts");
                } else {
                Log.d("MessageList", "No contacts created yet");
                }
                // Now load messages after contacts are loaded
                loadMessages();
            }

            @Override
            public void onContactAdded(Contact contact) {
                Log.d("MessageList", "Contact added: " + contact.getEmailAddress());
            }

            @Override
            public void onContactRemoved(Contact contact) {
                Log.d("MessageList", "Contact removed: " + contact.getEmailAddress());
            }

            @Override
            public void onError(String error) {
                Log.e("MessageList", "Error loading contacts: " + error);
                // Load messages even if contacts fail to load
                loadMessages();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Remove the listener when the activity is stopped
        if (messageListener != null && messagesQuery != null) {
            messagesQuery.removeEventListener(messageListener);
        }
    }

    private void loadMessages() {
        if (currentUserId == null) {
            binding.swipeRefreshLayout.setRefreshing(false);
            return;
        }

        // Show loading indicator
        binding.swipeRefreshLayout.setRefreshing(true);
        Log.d("MessageListActivity", "Loading messages for user: " + currentUserId);

        // Remove any existing listener to prevent duplicates
        if (messageListener != null && messagesQuery != null) {
            messagesQuery.removeEventListener(messageListener);
        }

        // Clear existing messages
        messages.clear();

        // Determine which messages to load
        String messageType = isInbox ? "received" : "sent";
        
        // Query messages
        messagesQuery = databaseHelper.getDatabaseReference()
                .child("user-messages")
                .child(currentUserId)
                .child(messageType)
                .orderByChild(currentSortField);

        if (!isAscending) {
            messagesQuery = messagesQuery.limitToLast(100); // Only get last 100 messages
        }

        // Add a value event listener for real-time updates
        messageListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d("MessageListActivity", "onDataChange, messages count: " + dataSnapshot.getChildrenCount());
                
                if (dataSnapshot.getChildrenCount() == 0) {
                    updateUI(new ArrayList<>());
                    return;
                }
                
                final List<Message> loadedMessages = new ArrayList<>();
                final int[] completedFetches = {0};
                final int totalMessages = (int) dataSnapshot.getChildrenCount();
                
                for (DataSnapshot messageRef : dataSnapshot.getChildren()) {
                    String messageId = messageRef.getKey();
                    Log.d("MessageListActivity", "Fetching message with ID: " + messageId);
                    
                    databaseHelper.getDatabaseReference()
                            .child("messages")
                            .child(messageId)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    Message message = snapshot.getValue(Message.class);
                                    if (message != null) {
                                        loadedMessages.add(message);
                                        completedFetches[0]++;

                                        if (completedFetches[0] == totalMessages) {
                                            // All messages loaded, sort and update UI
                                            if (currentSortField.equals("timestamp")) {
                                                if (!isAscending) {
                                                    Collections.reverse(loadedMessages);
                                                }
                                            } else if (currentSortField.equals("subject")) {
                                                loadedMessages.sort((m1, m2) -> {
                                                    String s1 = m1.getSubject() != null ? m1.getSubject() : "";
                                                    String s2 = m2.getSubject() != null ? m2.getSubject() : "";
                                                    return isAscending ? s1.compareTo(s2) : s2.compareTo(s1);
                                                });
                                            }
                                            updateUI(loadedMessages);
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.e("MessageListActivity", "Error loading message: " + error.getMessage());
                                    binding.swipeRefreshLayout.setRefreshing(false);
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("MessageListActivity", "Error loading messages: " + databaseError.getMessage());
                updateUI(new ArrayList<>());
            }
        };

        // Add the listener to the query
        messagesQuery.addValueEventListener(messageListener);
    }

    // Update the UI with new messages
    private void updateUI(List<Message> messages) {
        Log.d("MessageListActivity", "Updating UI with " + messages.size() + " messages");
        runOnUiThread(() -> {
            binding.swipeRefreshLayout.setRefreshing(false);

            if (messages == null || messages.isEmpty()) {
                binding.textEmpty.setVisibility(View.VISIBLE);
                binding.recyclerView.setVisibility(View.GONE);
            } else {
                binding.textEmpty.setVisibility(View.GONE);
                binding.recyclerView.setVisibility(View.VISIBLE);
                adapter.updateMessages(messages);
            }
        });
    }

    private void updateTitle() {
        String title = isInbox ? getString(R.string.label_inbox) : getString(R.string.label_outbox);
        binding.textViewTitle.setText(title);
    }

    private void updateUI() {
        binding.recyclerView.setVisibility(View.VISIBLE);
        binding.emptyView.setVisibility(View.GONE);
        binding.swipeRefreshLayout.setRefreshing(false);
        updateTitle();
    }

    // Action mode for multi-selection
    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            mode.getMenuInflater().inflate(R.menu.menu_message_actions, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false if nothing is done
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            // Handle action item clicks
            int id = item.getItemId();
            if (id == R.id.action_archive) {
                // TODO: Archive selected messages
                showSnackbar("Archive selected");
                mode.finish(); // Action picked, so close the CAB
                return true;
            } else if (id == R.id.action_set_reminder) {
                // TODO: Set reminder for selected messages
                showSnackbar("Set reminder selected");
                mode.finish();
                return true;
            } else if (id == R.id.action_delete) {
                // TODO: Delete selected messages
                showSnackbar("Delete selected");
                mode.finish();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
        }
    };

    private void showSnackbar(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
