package com.example.simplemessenger.ui.messaging;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.example.simplemessenger.ui.main.MainActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.simplemessenger.R;
import com.example.simplemessenger.data.ContactsManager;
import com.example.simplemessenger.data.DatabaseHelper;
import com.example.simplemessenger.data.MessageLoader;
import com.example.simplemessenger.data.model.Contact;
import com.example.simplemessenger.data.model.Message;
import com.example.simplemessenger.ui.auth.AuthActivity;
import com.example.simplemessenger.ui.config.FirebaseConfigActivity;
import com.example.simplemessenger.ui.contacts.ManageContactsActivity;
import com.example.simplemessenger.ui.adapters.MessageAdapter;
import com.example.simplemessenger.ui.settings.SettingsActivity;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import android.util.SparseArray;

class MessagesPagerAdapter extends FragmentStateAdapter {
    private static final String TAG = "MessagesPagerAdapter";
    private static final int NUM_PAGES = 3; // Inbox, Outbox, Notes
    private final SparseArray<MessageListFragment> fragments = new SparseArray<>();

    public MessagesPagerAdapter(FragmentActivity fa) {
        super(fa);
        Log.d(TAG, "Creating MessagesPagerAdapter");
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Log.d(TAG, "Creating fragment for position: " + position);
        
        // Create a new instance of MessageListFragment
        MessageListFragment fragment = new MessageListFragment();
        
        // Set up the fragment arguments
        Bundle args = new Bundle();
        args.putInt("position", position);
        args.putBoolean("isInbox", position == 0); // 0=Inbox, 1=Outbox, 2=Notes
        args.putBoolean("isNotes", position == 2); // Flag for notes tab
        fragment.setArguments(args);
        
        // Store the fragment reference
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
    private MessageLoader messageLoader;
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
        
        @Override
        public void onSelectionChanged(int selectedCount) {
            if (actionMode != null) {
                actionMode.setTitle(String.valueOf(selectedCount));
                actionMode.invalidate();
            }
        }
    }, isInbox);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check if user is authenticated
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e("MessageListActivity", "No authenticated user found, redirecting to login");
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }
        
        // Initialize the view binding
        binding = com.example.simplemessenger.databinding.ActivityMessageListWithPagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        try {
            Log.d("MessageListActivity", "Starting initialization in onCreate()");
            
            // Initialize Firebase instances
            Log.d("MessageListActivity", "Initializing DatabaseHelper...");
            databaseHelper = DatabaseHelper.getInstance();
            Log.d("MessageListActivity", "DatabaseHelper initialized: " + (databaseHelper != null));
            
            mAuth = FirebaseAuth.getInstance();
            currentUserId = currentUser.getUid();
            
            Log.d("MessageListActivity", "Current user ID: " + currentUserId);
            
            // Initialize ContactsManager after we have a valid user
            Log.d("MessageListActivity", "Initializing ContactsManager...");
            contactsManager = ContactsManager.getInstance();
            Log.d("MessageListActivity", "ContactsManager initialized: " + (contactsManager != null));
            
            // Initialize UI components
            Log.d("MessageListActivity", "Setting up UI...");
            setupUI();
            Log.d("MessageListActivity", "UI setup complete");
            
            // Initialize message loader
            Log.d("MessageListActivity", "Initializing message loader...");
            initializeMessageLoader();
            Log.d("MessageListActivity", "Message loader initialization attempted");
            
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
        
        // Set default to Inbox tab
        binding.viewPager.setCurrentItem(0, false);
        
        // Set up bottom navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_inbox) {
                // Already on messages screen (inbox)
                binding.viewPager.setCurrentItem(0, false);
                return true;
            } else if (itemId == R.id.navigation_outbox) {
                // Switch to outbox
                binding.viewPager.setCurrentItem(1, false);
                return true;
            } else if (itemId == R.id.navigation_notes) {
                // Switch to notes
                binding.viewPager.setCurrentItem(2, false);
                return true;
            } else if (itemId == R.id.navigation_contacts) {
                startActivity(new Intent(this, ManageContactsActivity.class));
                return true;
            }
            return false;
        });
        
        // Set inbox as selected by default
        bottomNav.setSelectedItemId(R.id.navigation_inbox);

        // Get the TabLayout from the binding
        TabLayout tabLayout = binding.tabLayout;
        if (tabLayout == null) {
            Log.e("MessageListActivity", "TabLayout not found in layout");
        }

        // Connect TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, binding.viewPager,
                (tab, position) -> {
                    String[] tabTitles = {
                        getString(R.string.label_inbox),
                        getString(R.string.label_outbox),
                        getString(R.string.label_notes)
                    };
                    tab.setText(tabTitles[position]);
                }).attach();

        // Set up tab selection listener
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                boolean newIsInbox = (tab.getPosition() == 0);
                if (newIsInbox != isInbox) {
                    isInbox = newIsInbox;
                    updateTitle();
                    // Reload messages for the selected tab
                    loadMessages();
                }
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
                boolean newIsInbox = (position == 0);
                if (newIsInbox != isInbox) {
                    isInbox = newIsInbox;
                    updateTitle();
                    // Reload messages for the selected tab
                    loadMessages();
                }
            }
        });
        
        // Select the initial tab (Inbox)
        if (tabLayout != null) {
            tabLayout.selectTab(tabLayout.getTabAt(0));
        } else {
            Log.e("MessageListActivity", "TabLayout is null, cannot set initial tab");
        }
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
        // Clean up the message loader when the activity is stopped
        if (messageLoader != null) {
            messageLoader.cleanup();
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
        } else if (id == R.id.action_contacts) {
            // Navigate to MainActivity which will handle the contacts tab
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("navigate_to_contacts", true);
            startActivity(intent);
            finish();
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
        Log.d("MessageListActivity", "loadMessages() called - USING FORCE LOAD");
        Log.d("MessageListActivity", "Thread: " + Thread.currentThread().getName());
        
        // Ensure we have the minimum required components
        if (databaseHelper == null) {
            Log.d("MessageListActivity", "Initializing databaseHelper in loadMessages");
            databaseHelper = DatabaseHelper.getInstance();
        }
        
        if (currentUserId == null) {
            Log.d("MessageListActivity", "Getting current user in loadMessages");
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                currentUserId = currentUser.getUid();
                Log.d("MessageListActivity", "Set currentUserId to: " + currentUserId);
            } else {
                String error = "Cannot load messages: No authenticated user";
                Log.e("MessageListActivity", error);
                runOnUiThread(() -> showSnackbar("Please sign in to view messages"));
                return;
            }
        }
        
        // Log current state
        Log.d("MessageListActivity", "loadMessages() - About to call forceLoadMessages()");
        Log.d("MessageListActivity", "Current user ID: " + currentUserId);
        Log.d("MessageListActivity", "Database helper initialized: " + (databaseHelper != null));
        
        // Use the direct loading method to bypass listener issues
        Log.d("MessageListActivity", "Calling forceLoadMessages() from loadMessages()");
        forceLoadMessages();
    }
    
    /*
    // Original listener-based implementation (commented out for now)
    public void loadMessagesOriginal() {
        Log.d("MessageListActivity", "loadMessages() called");
        
        if (!isInitialized()) {
            String error = "MessageLoader not properly initialized";
            Log.e("MessageListActivity", error);
            
            // Try to initialize the message loader again if it's null
            if (messageLoader == null) {
                Log.d("MessageListActivity", "Attempting to reinitialize message loader");
                initializeMessageLoader();
                
                // Check again if initialization was successful
                if (messageLoader == null) {
                    Log.e("MessageListActivity", "Failed to initialize message loader");
                    runOnUiThread(() -> {
                        if (binding != null) {
                            binding.swipeRefreshLayout.setRefreshing(false);
                        }
                        showSnackbar("Failed to initialize message loader. Please try again.");
                    });
                    return;
                }
            } else {
                runOnUiThread(() -> {
                    if (binding != null) {
                        binding.swipeRefreshLayout.setRefreshing(false);
                    }
                    showSnackbar(error);
                });
                return;
            }
        }

        // Show loading indicator
        runOnUiThread(() -> {
            if (binding != null) {
                binding.swipeRefreshLayout.setRefreshing(true);
            }
            Log.d("MessageListActivity", "Loading messages...");
        });

        try {
            // Clear existing messages and update UI immediately
            messages.clear();
            updateUI(messages);
            
            // Update message loader with current settings
            updateMessageLoaderForCurrentTab();
            
            Log.d("MessageListActivity", "Loading messages with params: " +
                    "isInbox=" + isInbox + ", " +
                    "sortField=" + currentSortField + ", " +
                    "isAscending=" + isAscending);
            
            // Log the current message loader state
            if (messageLoader == null) {
                Log.e("MessageListActivity", "MessageLoader is null right before loading messages!");
                runOnUiThread(() -> {
                    if (binding != null) {
                        binding.swipeRefreshLayout.setRefreshing(false);
                    }
                    showSnackbar("Error: Message loader not available");
                });
                return;
            }
            
            Log.d("MessageListActivity", "Calling messageLoader.loadMessages()");
            messageLoader.loadMessages(new MessageLoader.MessageLoadListener() {
                @Override
                public void onMessagesLoaded(List<Message> loadedMessages) {
                    Log.d("MessageListActivity", "onMessagesLoaded: " + 
                            (loadedMessages != null ? loadedMessages.size() : 0) + " messages");
                    
                    runOnUiThread(() -> {
                        try {
                            if (binding != null) {
                                binding.swipeRefreshLayout.setRefreshing(false);
                            }
                            messagesLoaded = true;
                            
                            if (loadedMessages != null && !loadedMessages.isEmpty()) {
                                Log.d("MessageListActivity", "Updating UI with " + loadedMessages.size() + " messages");
                                updateUI(loadedMessages);
                                updateFragments();
                            } else {
                                Log.d("MessageListActivity", "No messages to display");
                                updateUI(new ArrayList<>());
                            }
                        } catch (Exception e) {
                            Log.e("MessageListActivity", "Error in onMessagesLoaded", e);
                            showSnackbar("Error displaying messages");
                        }
                    });
                }
                
                @Override
                public void onError(String error) {
                    Log.e("MessageListActivity", "Error loading messages: " + error);
                    runOnUiThread(() -> {
                        try {
                            if (binding != null) {
                                binding.swipeRefreshLayout.setRefreshing(false);
                            }
                            showSnackbar("Error loading messages: " + error);
                            updateUI(new ArrayList<>());
                        } catch (Exception e) {
                            Log.e("MessageListActivity", "Error handling error state", e);
                        }
                    });
                }
            });
            
        } catch (Exception e) {
            Log.e("MessageListActivity", "Error in loadMessages", e);
            runOnUiThread(() -> {
                if (binding != null) {
                    binding.swipeRefreshLayout.setRefreshing(false);
                }
                showSnackbar("Error loading messages: " + e.getMessage());
                updateUI(new ArrayList<>());
            });
        }
    }
    */

    /**
     * Updates the UI with the provided messages
     * @param messages The messages to display (can be null or empty)
     */
    private void updateUI(List<Message> messages) {
        if (isFinishing() || isDestroyed()) {
            Log.w("MessageListActivity", "Activity is finishing or destroyed, skipping UI update");
            return;
        }
        
        final int messageCount = messages != null ? messages.size() : 0;
        Log.d("MessageListActivity", "Updating UI with " + messageCount + " messages, isInbox=" + isInbox);
        
        runOnUiThread(() -> {
            try {
                // Stop refresh animation if it's active
                if (binding != null) {
                    binding.swipeRefreshLayout.setRefreshing(false);
                }
                
                // Update messages list
                this.messages.clear();
                if (messages != null && !messages.isEmpty()) {
                    this.messages.addAll(messages);
                    
                    // Log first few messages for debugging
                    int maxMessagesToLog = Math.min(3, messages.size());
                    for (int i = 0; i < maxMessagesToLog; i++) {
                        Message msg = messages.get(i);
                        Log.d("MessageListActivity", "Message " + (i+1) + ": " + 
                                "id=" + msg.getId() + ", " +
                                "subject=" + msg.getSubject() + ", " +
                                "from=" + msg.getSenderId() + ", " +
                                "to=" + msg.getRecipientId());
                    }
                } else {
                    Log.d("MessageListActivity", "No messages to display");
                }
                
                // Mark messages as loaded
                messagesLoaded = true;
                
                // Update the title and UI state
                updateTitle();
                
                // Update all fragments with the new messages
                updateFragments();
                
            } catch (Exception e) {
                Log.e("MessageListActivity", "Error in UI update on UI thread", e);
                if (binding != null) {
                    binding.swipeRefreshLayout.setRefreshing(false);
                }
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
    /**
     * Directly loads messages from the database without using listeners
     * This is a temporary solution for debugging UI issues
     */
    private void forceLoadMessages() {
        final String TAG = "MessageListActivity";
        Log.d(TAG, "forceLoadMessages() called - Starting message loading process");
        Log.d(TAG, "Thread: " + Thread.currentThread().getName());
        Log.d(TAG, "Current state - isInbox: " + isInbox + 
                  ", currentUserId: " + currentUserId + 
                  ", databaseHelper: " + (databaseHelper != null));
                  
        if (databaseHelper == null) {
            Log.e(TAG, "DatabaseHelper is null, cannot load messages");
            runOnUiThread(() -> showSnackbar("Error: Database not available"));
            return;
        }
        
        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.e(TAG, "Current user ID is not set, cannot load messages");
            runOnUiThread(() -> showSnackbar("Please sign in to view messages"));
            return;
        }
        
        if (!isInitialized()) {
            Log.e(TAG, "MessageLoader not properly initialized");
            return;
        }
        
        // Show loading indicator
        runOnUiThread(() -> {
            if (binding != null) {
                binding.swipeRefreshLayout.setRefreshing(true);
            }
        });
        
        // Clear existing messages
        messages.clear();
        
        // Get database reference
        DatabaseReference messagesRef = databaseHelper.getDatabaseReference()
                .child("user-messages")
                .child(currentUserId)
                .child(isInbox ? "received" : "sent");
        
        Log.d(TAG, "Loading messages from path: " + messagesRef.toString());
        
        // Use a single value event listener for a one-time fetch
        messagesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "onDataChange: Found " + dataSnapshot.getChildrenCount() + " message references");
                
                if (!dataSnapshot.exists() || dataSnapshot.getChildrenCount() == 0) {
                    Log.d(TAG, "No messages found for user");
                    runOnUiThread(() -> {
                        updateUI(new ArrayList<>());
                        if (binding != null) {
                            binding.swipeRefreshLayout.setRefreshing(false);
                        }
                    });
                    return;
                }
                
                // List to hold all loaded messages
                List<Message> loadedMessages = new ArrayList<>();
                final int[] pendingFetches = { (int) dataSnapshot.getChildrenCount() };
                
                // For each message reference, fetch the actual message
                for (DataSnapshot messageRef : dataSnapshot.getChildren()) {
                    String messageId = messageRef.getKey();
                    if (messageId == null) continue;
                    
                    DatabaseReference messageRefFull = databaseHelper.getDatabaseReference()
                            .child("messages")
                            .child(messageId);
                    
                    messageRefFull.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot messageSnapshot) {
                            if (messageSnapshot.exists()) {
                                Message message = messageSnapshot.getValue(Message.class);
                                if (message != null) {
                                    message.setId(messageSnapshot.getKey());
                                    synchronized (loadedMessages) {
                                        loadedMessages.add(message);
                                    }
                                }
                            }
                            
                            // Check if all fetches are complete
                            synchronized (pendingFetches) {
                                pendingFetches[0]--;
                                if (pendingFetches[0] == 0) {
                                    // All messages loaded, update UI
                                    Log.d(TAG, "All messages loaded, count: " + loadedMessages.size());
                                    runOnUiThread(() -> {
                                        updateUI(loadedMessages);
                                        if (binding != null) {
                                            binding.swipeRefreshLayout.setRefreshing(false);
                                        }
                                    });
                                }
                            }
                        }
                        
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Error loading message: " + error.getMessage());
                            synchronized (pendingFetches) {
                                pendingFetches[0]--;
                                if (pendingFetches[0] == 0) {
                                    runOnUiThread(() -> {
                                        updateUI(loadedMessages);
                                        if (binding != null) {
                                            binding.swipeRefreshLayout.setRefreshing(false);
                                        }
                                    });
                                }
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading messages: " + error.getMessage());
                runOnUiThread(() -> {
                    showSnackbar("Error loading messages: " + error.getMessage());
                    if (binding != null) {
                        binding.swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
        });
    }
    
    /**
     * Updates all fragments with the current messages
     */
    private void updateFragments() {
        final String TAG = "MessageListActivity";
        Log.d(TAG, "updateFragments() called with " + messages.size() + " messages, isInbox=" + isInbox);
        
        try {
            // Check if pagerAdapter is initialized
            if (pagerAdapter == null) {
                Log.e(TAG, "pagerAdapter is null in updateFragments");
                return;
            }
            
            // Check if activity is finishing or destroyed
            if (isFinishing() || isDestroyed()) {
                Log.w(TAG, "Activity is finishing or destroyed, skipping fragment updates");
                return;
            }
            
            // Check if binding and view pager are valid
            if (binding == null || binding.viewPager == null) {
                Log.e(TAG, "Binding or ViewPager is null, cannot update fragments");
                return;
            }
            
            int itemCount = pagerAdapter.getItemCount();
            Log.d(TAG, "Updating " + itemCount + " fragments, total messages: " + messages.size() + 
                    ", isInbox: " + isInbox);
            
            // Get current view pager position for reference
            int currentPosition = binding.viewPager.getCurrentItem();
            Log.d(TAG, "Current view pager position: " + currentPosition);
            
            // Update both fragments (inbox and outbox)
            for (int i = 0; i < itemCount; i++) {
                try {
                    Log.d(TAG, "Processing fragment at position: " + i);
                    
                    // Get the fragment from pager adapter
                    MessageListFragment fragment = pagerAdapter.getFragment(i);
                    if (fragment == null) {
                        Log.e(TAG, "Fragment at position " + i + " is null");
                        continue;
                    }
                    
                    // Check fragment state
                    if (!fragment.isAdded()) {
                        Log.w(TAG, "Fragment at position " + i + " is not added to activity");
                        continue;
                    }
                    
                    if (fragment.isDetached()) {
                        Log.w(TAG, "Fragment at position " + i + " is detached from activity");
                        continue;
                    }
                    
                    if (fragment.isRemoving()) {
                        Log.w(TAG, "Fragment at position " + i + " is being removed");
                        continue;
                    }
                    
                    // Filter messages for this fragment (inbox or outbox)
                    boolean isInboxFragment = (i == 0);
                    List<Message> filteredMessages = filterMessages(messages, isInboxFragment);
                    
                    Log.d(TAG, String.format("Updating fragment %d (isInbox: %b) with %d messages", 
                            i, isInboxFragment, filteredMessages != null ? filteredMessages.size() : 0));
                    
                    // Log first few messages for debugging
                    if (filteredMessages != null && !filteredMessages.isEmpty()) {
                        int logCount = Math.min(3, filteredMessages.size());
                        for (int j = 0; j < logCount; j++) {
                            Message msg = filteredMessages.get(j);
                            Log.d(TAG, String.format("Message %d: id=%s, from=%s, to=%s, subject=%s", 
                                    j, msg.getId(), msg.getSenderId(), 
                                    msg.getRecipientId(), msg.getSubject()));
                        }
                    }
                    
                    // Update the fragment with the filtered messages
                    if (filteredMessages != null) {
                        fragment.updateMessages(filteredMessages);
                    } else {
                        Log.e(TAG, "Filtered messages is null for fragment " + i);
                        fragment.updateMessages(new ArrayList<>());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error updating fragment " + i, e);
                }
            }
            
            Log.d(TAG, "Fragment update completed");
            
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in updateFragments", e);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("MessageListActivity", "onStart() called");
        
        // Check if user is still authenticated
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e("MessageListActivity", "User logged out, finishing activity");
            finish();
            return;
        }
        
        // Update current user ID if needed
        if (currentUserId == null) {
            currentUserId = currentUser.getUid();
            Log.d("MessageListActivity", "Set currentUserId to: " + currentUserId);
        }
        
        // Initialize components if needed
        if (databaseHelper == null) {
            databaseHelper = DatabaseHelper.getInstance();
            Log.d("MessageListActivity", "Initialized databaseHelper");
        }
        
        if (contactsManager == null) {
            contactsManager = ContactsManager.getInstance();
            Log.d("MessageListActivity", "Initialized contactsManager");
        }
        
        // Log initialization state before trying to load messages
        Log.d("MessageListActivity", "Checking initialization state...");
        Log.d("MessageListActivity", "messageLoader: " + (messageLoader != null));
        Log.d("MessageListActivity", "databaseHelper: " + (databaseHelper != null));
        Log.d("MessageListActivity", "contactsManager: " + (contactsManager != null));
        Log.d("MessageListActivity", "currentUserId: " + (currentUserId != null));
        Log.d("MessageListActivity", "binding: " + (binding != null));
        
        // Initialize message loader if needed
        if (messageLoader == null) {
            Log.d("MessageListActivity", "Initializing messageLoader");
            initializeMessageLoader();
        } else {
            Log.d("MessageListActivity", "messageLoader already initialized");
        }
        
        // Log initialization state after potential messageLoader initialization
        Log.d("MessageListActivity", "Initialization state before loadMessages():" +
                "\n  databaseHelper: " + (databaseHelper != null) +
                "\n  messageLoader: " + (messageLoader != null) +
                "\n  currentUserId: " + currentUserId);
        
        // Call loadMessages() directly to ensure it's called
        Log.d("MessageListActivity", "Calling loadMessages() from onStart");
        loadMessages();
        
        // Also try loading contacts and messages
        Log.d("MessageListActivity", "Calling loadContactsAndMessages() from onStart");
        loadContactsAndMessages();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up resources
        if (messageLoader != null) {
            messageLoader.cleanup();
            messageLoader = null;
        }
        binding = null;
    }
    
    /**
     * Initializes the message loader with current settings
     */
    private void setupUI() {
        // Set up the toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(isInbox ? R.string.label_inbox : R.string.label_outbox);
        }
        
        // Set up ViewPager and TabLayout
        pagerAdapter = new MessagesPagerAdapter(this);
        binding.viewPager.setAdapter(pagerAdapter);
        
        new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) -> {
            tab.setText(position == 0 ? getString(R.string.label_inbox) : getString(R.string.label_sent));
        }).attach();
        
        // Set up pull-to-refresh
        binding.swipeRefreshLayout.setOnRefreshListener(this::refreshMessages);
    }
    
    private void initializeMessageLoader() {
        Log.d("MessageListActivity", "initializeMessageLoader() called");
        
        if (databaseHelper == null) {
            Log.e("MessageListActivity", "Cannot initialize message loader - databaseHelper is null");
            return;
        }
        if (contactsManager == null) {
            Log.e("MessageListActivity", "Cannot initialize message loader - contactsManager is null");
            return;
        }
        if (currentUserId == null) {
            Log.e("MessageListActivity", "Cannot initialize message loader - currentUserId is null");
            return;
        }
        
        // Don't reinitialize if already initialized
        if (messageLoader != null) {
            Log.d("MessageListActivity", "MessageLoader already initialized");
            return;
        }
        
        Log.d("MessageListActivity", "Creating new MessageLoader instance");
        try {
            messageLoader = new MessageLoader(
                databaseHelper,
                contactsManager,
                currentUserId,
                isInbox,
                currentSortField,
                isAscending
            );
            Log.d("MessageListActivity", "MessageLoader created successfully");
        } catch (Exception e) {
            Log.e("MessageListActivity", "Error creating MessageLoader", e);
            messageLoader = null;
        }
    }
    
    /**
     * Updates the message loader with the current tab and sort settings
     */
    private void updateMessageLoaderForCurrentTab() {
        if (messageLoader != null) {
            messageLoader.setInboxMode(isInbox);
            messageLoader.setSortOrder(currentSortField, isAscending);
        } else {
            Log.e("MessageListActivity", "Cannot update message loader - not initialized");
        }
    }
    
    /**
     * Checks if all required components are initialized
     */
    private boolean isInitialized() {
        boolean initialized = messageLoader != null && 
                           databaseHelper != null && 
                           contactsManager != null && 
                           currentUserId != null &&
                           binding != null;
        
        Log.d("MessageListActivity", "isInitialized(): " + initialized + 
                " (messageLoader: " + (messageLoader != null) + 
                ", databaseHelper: " + (databaseHelper != null) + 
                ", contactsManager: " + (contactsManager != null) + 
                ", currentUserId: " + (currentUserId != null) + 
                ", binding: " + (binding != null) + ")");
                
        return initialized;
    }
}
