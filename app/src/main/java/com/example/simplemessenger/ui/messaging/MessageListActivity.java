package com.example.simplemessenger.ui.messaging;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.simplemessenger.R;
import com.example.simplemessenger.data.ContactsManager;
import com.example.simplemessenger.data.DatabaseHelper;
import com.example.simplemessenger.data.model.Contact;
import com.example.simplemessenger.data.model.Message;
import com.example.simplemessenger.databinding.ActivityMessageListBinding;
import com.example.simplemessenger.ui.auth.AuthActivity;
import com.example.simplemessenger.ui.config.FirebaseConfigActivity;
import com.example.simplemessenger.ui.contacts.ManageContactsActivity;
import com.example.simplemessenger.ui.messaging.adapter.MessageAdapter;
import com.example.simplemessenger.ui.profile.ProfileActivity;
import com.example.simplemessenger.ui.settings.SettingsActivity;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import android.util.SparseArray;

class MessagesPagerAdapter extends FragmentStateAdapter {
    private static final int NUM_PAGES = 2;
    private final SparseArray<MessageListFragment> fragments = new SparseArray<>();

    public MessagesPagerAdapter(FragmentActivity fa) {
        super(fa);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        MessageListFragment fragment = MessageListFragment.newInstance(position);
        fragments.put(position, fragment);
        return fragment;
    }

    @Override
    public int getItemCount() {
        return NUM_PAGES;
    }

    @Nullable
    public MessageListFragment getFragment(int position) {
        return fragments.get(position);
    }
}

public class MessageListActivity extends AppCompatActivity {

    private com.example.simplemessenger.databinding.ActivityMessageListWithPagerBinding binding;
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
    private MessagesPagerAdapter pagerAdapter;
    private boolean messagesLoaded = false; // Track if messages have been loaded

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
    }, isInbox);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = com.example.simplemessenger.databinding.ActivityMessageListWithPagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        try {
            // Initialize Firebase instances
            databaseHelper = DatabaseHelper.getInstance();
            mAuth = FirebaseAuth.getInstance();
            
            // Get current user
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                currentUserId = currentUser.getUid();
                Log.d("MessageListActivity", "Current user ID: " + currentUserId);
                
                // Initialize ContactsManager after we have a valid user
                contactsManager = ContactsManager.getInstance();
                Log.d("MessageListActivity", "ContactsManager initialized");
                
                // UI setup code will continue below
            } else {
                Log.e("MessageListActivity", "No authenticated user found");
                finish(); // Close the activity if user is not authenticated
            }
        } catch (Exception e) {
            Log.e("MessageListActivity", "Error during initialization", e);
            showSnackbar("Error initializing app. Please try again.");
            finish();
        }

        // Set up toolbar
        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        
        // Enable home/up button and show title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back);
        }

        // Set up ViewPager2 with TabLayout
        pagerAdapter = new MessagesPagerAdapter(this);
        binding.viewPager.setAdapter(pagerAdapter);
        binding.viewPager.setOffscreenPageLimit(2); // Keep both pages in memory

        // Find the TabLayout inside the Toolbar
        TabLayout tabLayout = binding.toolbar.findViewById(R.id.tabLayout);
        if (tabLayout == null) {
            Log.e("MessageListActivity", "TabLayout not found in Toolbar");
        }

        // Connect TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, binding.viewPager,
                (tab, position) -> {
                    tab.setText(position == 0 ? getString(R.string.label_inbox) : getString(R.string.label_outbox));
                }).attach();

        // Set up tab selection listener
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                isInbox = (tab.getPosition() == 0);
                updateTitle();
                // Update the current fragment with the correct messages
                updateFragments();
            }


            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Set up FAB click listener
        binding.fab.setOnClickListener(v -> startActivity(new Intent(this, ComposeMessageActivity.class)));

        // Set up swipe to refresh
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            // Force refresh messages from the database
            loadMessages();
        });

        // Set initial title
        updateTitle();

        // Set up page change callback
        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                isInbox = (position == 0);
                updateTitle();
                // Update fragments with the current messages
                updateFragments();
            }
        });
        
        // Select the initial tab (Inbox)
        if (tabLayout != null) {
            tabLayout.selectTab(tabLayout.getTabAt(0));
        } else {
            Log.e("MessageListActivity", "TabLayout is null, cannot set initial tab");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadContactsAndMessages();
        
        // Post a message to the handler to ensure fragments are created
//        new Handler(Looper.getMainLooper()).postDelayed(() -> {
//            // Load messages for the current tab
//            loadMessages();
//        }, 100);
    }

    private void loadContactsAndMessages() {
        Log.d("MessageList", "Starting loadContactsAndMessages");
        
        if (contactsManager == null) {
            Log.e("MessageList", "ContactsManager is null, cannot load contacts");
            loadMessages(); // Try to load messages anyway
            return;
        }

        // First, set up the listener
        ContactsManager.ContactsLoadListener contactsListener = new ContactsManager.ContactsLoadListener() {
            @Override
            public void onContactsLoaded(List<Contact> contacts) {
                Log.d("MessageList", "onContactsLoaded called with " + (contacts != null ? contacts.size() : 0) + " contacts");
                messagesLoaded = true;
                // Now load messages after contacts are loaded
                loadMessages();
            }
            
            @Override
            public void onContactAdded(Contact contact) {
                Log.d("MessageList", "Contact added: " + (contact != null ? contact.getEmailAddress() : "null"));
            }

            @Override
            public void onContactRemoved(Contact contact) {
                Log.d("MessageList", "Contact removed: " + (contact != null ? contact.getEmailAddress() : "null"));
            }

            @Override
            public void onError(String error) {
                Log.e("MessageList", "Error in contacts listener: " + error);
                // Load messages even if contacts fail to load
                loadMessages();
            }
        };
        
        try {
            // Ensure we have a valid user ID
            if (currentUserId == null) {
                Log.e("MessageList", "Current user ID is null, cannot load contacts");
                loadMessages(); // Try to load messages anyway
                return;
            }
            
            // Set the listener first
            Log.d("MessageList", "Setting contacts load listener");
            contactsManager.setLoadListener(contactsListener);
            
            // Initialize contacts
            Log.d("MessageList", "Initializing contacts");
            contactsManager.initializeContacts();
            
            // Add a timeout to ensure messages load even if contacts don't respond
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (!messagesLoaded) {
                    Log.w("MessageList", "Contacts load timeout, loading messages anyway");
                    loadMessages();
                }
            }, 3000); // 3 second timeout
            
        } catch (Exception e) {
            Log.e("MessageList", "Error in loadContactsAndMessages: " + e.getMessage(), e);
            // If anything goes wrong, try to load messages anyway
            loadMessages();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Remove the listener when the activity is stopped
        if (messageListener != null && messagesQuery != null) {
            messagesQuery.removeEventListener(messageListener);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // Handle home/up button press
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        } else if (id == R.id.action_contacts) {
            startActivity(new Intent(this, ManageContactsActivity.class));
            return true;
        } else if (id == R.id.action_logout) {
            // Handle logout
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return true;
        } else if (id == R.id.action_firebase_settings) {
            startActivity(new Intent(this, FirebaseConfigActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void loadMessages() {
        Log.d("MessageList", "Starting loadMessages");
        messagesLoaded = false;
        
        if (currentUserId == null) {
            String error = "Current user ID is null. User not authenticated?";
            Log.e("MessageList", error);
            binding.swipeRefreshLayout.setRefreshing(false);
            showSnackbar(error);
            return;
        }

        // Show loading indicator
        binding.swipeRefreshLayout.setRefreshing(true);
        String logMessage = "Loading " + (isInbox ? "inbox" : "outbox") + " messages for user: " + currentUserId;
        Log.d("MessageList", logMessage);
        showSnackbar(logMessage);

        // Remove any existing listener to prevent duplicates
        if (messageListener != null && messagesQuery != null) {
            messagesQuery.removeEventListener(messageListener);
        }

        // Clear existing messages and update UI immediately
        messages.clear();
        updateUI(messages);

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
                    Log.d("MessageListActivity", "No " + messageType + " messages found for user");
                    updateUI(new ArrayList<>());
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

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("MessageListActivity", "Error checking messages reference: " + error.getMessage());
                binding.swipeRefreshLayout.setRefreshing(false);
                updateUI(new ArrayList<>());
            }
        });
    }

    // Update the UI with new messages
    private void updateUI(List<Message> messages) {
        Log.d("MessageListActivity", "Updating UI with " + (messages != null ? messages.size() : 0) + " messages");
        runOnUiThread(() -> {
            try {
                binding.swipeRefreshLayout.setRefreshing(false);
                this.messages.clear();
                this.messages.addAll(messages != null ? messages : new ArrayList<>());
                
                // Mark messages as loaded
                messagesLoaded = true;
                Log.d("MessageListActivity", "Messages loaded: " + this.messages.size() + " messages");
                
                // Update all fragments with the new messages
                updateFragments();
            } catch (Exception e) {
                Log.e("MessageListActivity", "Error updating UI", e);
                messagesLoaded = false;
            }
        });
    }
    
    // Helper method to filter messages for a specific tab
    private List<Message> filterMessages(List<Message> messages, boolean isInbox) {
        if (messages == null) {
            return new ArrayList<>();
        }
        
        List<Message> filtered = new ArrayList<>();
        String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
        
        for (Message message : messages) {
            if (isInbox) {
                // For inbox, show messages where current user is the recipient
                if (message.getRecipientId() != null && message.getRecipientId().equals(currentUserId)) {
                    filtered.add(message);
                }
            } else {
                // For outbox, show messages where current user is the sender
                if (message.getSenderId() != null && message.getSenderId().equals(currentUserId)) {
                    filtered.add(message);
                }
            }
        }
        
        return filtered;
    }
    
    // Filter messages based on the current tab (inbox/outbox)
    private List<Message> filterMessagesForCurrentTab(List<Message> allMessages) {
        return filterMessages(allMessages, isInbox);
    }
    
    // Method to be called when inbox/outbox is toggled in the UI
    public void onInboxOutboxToggled(boolean isInbox) {
        // Update the current tab and reload messages
        int newTab = isInbox ? 0 : 1;
        binding.viewPager.setCurrentItem(newTab, true);
    }
    
    // Method to refresh messages (for pull-to-refresh)
    public void refreshMessages() {
        loadMessages();
    }

    private void updateTitle() {
        String title = isInbox ? getString(R.string.label_inbox) : getString(R.string.label_outbox);
        binding.textViewTitle.setText(title);
    }

    private void updateUI() {
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
    
    /**
     * Updates all fragments with the current messages
     */
    private void updateFragments() {
        if (pagerAdapter == null) return;
        
        for (int i = 0; i < pagerAdapter.getItemCount(); i++) {
            MessageListFragment fragment = pagerAdapter.getFragment(i);
            if (fragment != null && fragment.isAdded()) {
                boolean isInboxFragment = (i == 0);
                List<Message> filteredMessages = filterMessages(messages, isInboxFragment);
                fragment.updateMessages(filteredMessages);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
