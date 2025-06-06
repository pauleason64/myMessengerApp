package com.example.simplemessenger.ui.messaging;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import android.app.AlertDialog;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.simplemessenger.R;
import com.example.simplemessenger.databinding.FragmentMessageListBinding;
import com.example.simplemessenger.data.DatabaseHelper;
import com.example.simplemessenger.data.model.Message;
import com.example.simplemessenger.ui.adapters.MessageAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.example.simplemessenger.util.FirebaseFactory;
import com.google.firebase.database.ValueEventListener;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;


public class MessageListFragment extends Fragment {
    private static final String ARG_POSITION = "position";
    private RecyclerView recyclerView;
    private List<Message> messages = new ArrayList<>();
    private boolean isInbox = true;
    private boolean isNotes = false;
    private int position = 0;
    private FragmentMessageListBinding binding;
    private MessageAdapter adapter;
    private DatabaseHelper databaseHelper;
    private MenuItem trashMenuItem;
    private boolean isMultiSelectMode = false;
    
    // Timeout handling
    private static final long DATABASE_TIMEOUT_MS = 15000; // 15 seconds
    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;
    
    private final androidx.appcompat.view.ActionMode.Callback actionModeCallback = new androidx.appcompat.view.ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(androidx.appcompat.view.ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.menu_message_list, menu);
            trashMenuItem = menu.findItem(R.id.action_delete);
            if (trashMenuItem != null) {
                trashMenuItem.setVisible(true);
                trashMenuItem.setEnabled(false);
            }
            return true;
        }

        @Override
        public boolean onPrepareActionMode(androidx.appcompat.view.ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(androidx.appcompat.view.ActionMode mode, MenuItem item) {
            int id = item.getItemId();
            if (id == R.id.action_delete) {
                deleteSelectedMessages();
                mode.finish();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(androidx.appcompat.view.ActionMode mode) {
            exitMultiSelectMode();
        }
    };

    public MessageListFragment() {
    }

    public static MessageListFragment newInstance(int position) {
        MessageListFragment fragment = new MessageListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_POSITION, position);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize DatabaseHelper
        databaseHelper = DatabaseHelper.getInstance();
        
        // Set isInbox and isNotes based on the position from arguments
        if (getArguments() != null) {
            position = getArguments().getInt(ARG_POSITION, 0);
            isInbox = (position == 0); // 0 for inbox, 1 for outbox, 2 for notes
            isNotes = (position == 2);
            
            // If it's notes tab, we're not in inbox or outbox
            if (isNotes) {
                isInbox = false;
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                         Bundle savedInstanceState) {
        binding = FragmentMessageListBinding.inflate(inflater, container, false);
        setHasOptionsMenu(true);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Update the tab state in DatabaseHelper
        if (databaseHelper != null) {
            databaseHelper.setTabState(isNotes, isInbox);
        }
        
        // Ensure we have the latest arguments
        if (getArguments() != null) {
            position = getArguments().getInt(ARG_POSITION, 0);
            isInbox = (position == 0); // 0 for inbox, 1 for outbox, 2 for notes
            isNotes = (position == 2);
            
            // If it's notes tab, we're not in inbox or outbox
            if (isNotes) {
                isInbox = false;
            }
        }
        
        Log.d("MessageListFragment", "onViewCreated - position: " + position + ", isInbox: " + isInbox + ", isNotes: " + isNotes);
        
        // Initialize views
        recyclerView = binding.recyclerView;
        setupRecyclerView();
        
        // Initialize the adapter with the action listener
        adapter = new MessageAdapter(new MessageAdapter.OnMessageActionListener() {
            @Override
            public void onMessageSelected(Message message) {
                if (isMultiSelectMode) {
                    // In multi-select mode, toggle selection on click
                    adapter.toggleSelection(message.getId());
                } else {
                    // In normal mode, open message details
                    if (getActivity() != null && message != null) {
                        String messageId = message.getId();
                        if (messageId != null && !messageId.isEmpty()) {
                            Log.d("MessageListFragment", "Opening message with ID: " + messageId);
                            Intent intent = new Intent(getActivity(), MessageDetailActivity.class);
                            intent.putExtra("message_id", messageId);
                            startActivity(intent);
                        }
                    }
                }
            }

            @Override
            public void onMessageLongClicked(Message message) {
                // Enter multi-select mode on long click
                if (!isMultiSelectMode) {
                    isMultiSelectMode = true;
                    adapter.setMultiSelectMode(true);
                    
                    // Start the action mode
                    if (getActivity() != null) {
                        ((AppCompatActivity) getActivity()).startSupportActionMode(actionModeCallback);
                    }
                    
                    if (trashMenuItem != null) {
                        trashMenuItem.setVisible(true);
                    }
                }
                // Toggle selection of the clicked item
                adapter.toggleSelection(message.getId());
            }

            @Override
            public void onSelectionChanged(int selectedCount) {
                // Update UI based on selection count
                if (trashMenuItem != null) {
                    trashMenuItem.setEnabled(selectedCount > 0);
                }
                
                // If no items selected, exit multi-select mode
                if (selectedCount == 0 && isMultiSelectMode) {
                    exitMultiSelectMode();
                }
            }
        }, isInbox);
        
        recyclerView.setAdapter(adapter);
        
        // Hide the radio group as we're using bottom navigation for tab switching
        if (binding.radioGroup != null) {
            binding.radioGroup.setVisibility(View.GONE);
        }
        
        // Set up FAB with appropriate icon and click listener
        if (binding.fabCompose != null) {
            binding.fabCompose.setImageResource(isNotes ? R.drawable.ic_note_add : R.drawable.ic_add);
            binding.fabCompose.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), ComposeMessageActivity.class);
                if (isNotes) {
                    // For notes tab, set both note flags
                    intent.putExtra(ComposeMessageActivity.EXTRA_IS_NOTE, true)
                           .putExtra(ComposeMessageActivity.EXTRA_NOTE_MODE, true);
                } else {
                    // For inbox/outbox, just mark as new message
                    intent.putExtra(ComposeMessageActivity.EXTRA_IS_NOTE, false)
                           .putExtra(ComposeMessageActivity.EXTRA_COMPOSE_NEW, true);
                }
                startActivity(intent);
            });
        }
        
        // Set up swipe to refresh with pull-to-refresh
        if (binding.swipeRefreshLayout != null) {
            binding.swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
            );
            
            binding.swipeRefreshLayout.setOnRefreshListener(() -> {
                Log.d("MessageListFragment", "Pull to refresh triggered");
                loadMessagesDirectly();
            });
        }
        
        // Show loading indicator initially
        showLoadingIndicator(true);
        
        // Load messages directly
        Log.d("MessageListFragment", "Loading messages for tab: " + (isNotes ? "Notes" : (isInbox ? "Inbox" : "Outbox")));
        loadMessagesDirectly();
        
        // Update UI based on current tab
        updateUIForCurrentTab();
    }
    
    private void loadMessagesDirectly() {
        // Cancel any existing timeout
        if (timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
            timeoutRunnable = null;
        }
        
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e("MessageListFragment", "User not authenticated");
            showLoadingIndicator(false);
            if (binding != null) {
                binding.swipeRefreshLayout.setRefreshing(false);
            }
            return;
        }
        
        String currentUserId = currentUser.getUid();
        Log.d("MessageListFragment", "Loading messages for user: " + currentUserId + ", isNotes: " + isNotes + ", isInbox: " + isInbox);
        
        // Clear previous messages
        messages.clear();
        if (adapter != null) {
            adapter.updateMessages(new ArrayList<>());
        }
        
        // Show loading indicator
        showLoadingIndicator(true);
        
        // Get the appropriate database reference based on the current tab
        DatabaseReference rootRef = FirebaseFactory.getDatabase().getReference();
        Log.d("MessageListFragment", "Root reference: " + rootRef.toString());
        
        // Note: Using hyphen to match security rules
        DatabaseReference userMessagesRef = rootRef.child("user-messages");
        Log.d("MessageListFragment", "user_messages reference: " + userMessagesRef.toString());
        
        DatabaseReference userRef = userMessagesRef.child(currentUserId);
        Log.d("MessageListFragment", "User reference: " + userRef.toString() + " (UID: " + currentUserId + ")");
                
        DatabaseReference messagesRef = rootRef.child("messages");
        Log.d("MessageListFragment", "Messages reference: " + messagesRef.toString());
        
        DatabaseReference tabRef;
        
        if (isNotes) {
            tabRef = userRef.child("notes");
            Log.d("MessageListFragment", "Loading notes from: " + tabRef.toString());
        } else if (isInbox) {
            tabRef = userRef.child("received");
            Log.d("MessageListFragment", "Loading received messages from: " + tabRef.toString());
        } else {
            tabRef = userRef.child("sent");
            Log.d("MessageListFragment", "Loading sent messages from: " + tabRef.toString());
        }
        
        // Ensure we have a valid reference
        if (tabRef == null) {
            Log.e("MessageListFragment", "Failed to get database reference for messages");
            showLoadingIndicator(false);
            if (binding != null) {
                binding.swipeRefreshLayout.setRefreshing(false);
            }
            updateUIForEmptyState();
            return;
        }
        
        // Use a counter to track the query
        final int[] queriesCompleted = {0};
        final List<Message> allMessages = new ArrayList<>();
        
        Log.d("MessageListFragment", "Loading from path: " + tabRef.toString());
        
        // Set up timeout
        timeoutRunnable = () -> {
            Log.e("MessageListFragment", "Database operation timed out after " + DATABASE_TIMEOUT_MS + "ms");
            if (getActivity() != null && !getActivity().isFinishing()) {
                getActivity().runOnUiThread(() -> {
                    showLoadingIndicator(false);
                    if (binding != null) {
                        binding.swipeRefreshLayout.setRefreshing(false);
                        binding.textEmpty.setVisibility(View.VISIBLE);
                        binding.textEmpty.setText(R.string.error_loading_messages);
                    }
                    Toast.makeText(getContext(), "Connection timed out. Please check your internet connection and try again.", 
                            Toast.LENGTH_LONG).show();
                });
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, DATABASE_TIMEOUT_MS);
        
        // Load messages for the current tab
        Log.d("MessageListFragment", "Adding value event listener to: " + tabRef.toString());
        Log.d("MessageListFragment", "Current user UID: " + currentUserId);
        Log.d("MessageListFragment", "Database URL: " + FirebaseFactory.getDatabaseUrl());
        tabRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Cancel the timeout since we got a response
                if (timeoutRunnable != null) {
                    timeoutHandler.removeCallbacks(timeoutRunnable);
                    timeoutRunnable = null;
                }
                
                // Log the raw snapshot value to see what's actually in the database
                Log.d("MessageListFragment", "Raw snapshot value: " + dataSnapshot.getValue());
                Log.d("MessageListFragment", "Snapshot exists: " + dataSnapshot.exists());
                Log.d("MessageListFragment", "onDataChange called with " + dataSnapshot.getChildrenCount() + " children");
                
                // Log each child key and value
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    Log.d("MessageListFragment", "Child key: " + child.getKey() + 
                                            ", value: " + child.getValue() + 
                                            ", has children: " + (child.getChildren().iterator().hasNext()));
                }
                
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser == null) {
                    Log.e("MessageListFragment", "Current user is null in onDataChange");
                    return;
                }
                
                String currentUserId = currentUser.getUid();
                final int totalMessages = (int) dataSnapshot.getChildrenCount();
                Log.d("MessageListFragment", "Found " + totalMessages + " message references in " + (isNotes ? "notes" : (isInbox ? "inbox" : "sent")));
                
                if (totalMessages == 0) {
                    Log.d("MessageListFragment", "No message references found. Checking parent node...");
                    // Try to check if we can access the parent node
                    tabRef.getParent().addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot parentSnapshot) {
                            Log.d("MessageListFragment", "Parent node exists: " + parentSnapshot.exists());
                            Log.d("MessageListFragment", "Parent node value: " + parentSnapshot.getValue());
                            checkAllQueriesCompleted(++queriesCompleted[0], allMessages);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("MessageListFragment", "Error checking parent node: " + error.getMessage());
                            checkAllQueriesCompleted(++queriesCompleted[0], allMessages);
                        }
                    });
                    return;
                }
                
                final int[] loadedCount = {0};
                
                for (DataSnapshot messageRef : dataSnapshot.getChildren()) {
                    String messageId = messageRef.getKey();
                    if (messageId == null) {
                        Log.d("MessageListFragment", "Skipping null message ID");
                        continue;
                    }
                    Log.d("MessageListFragment", "Processing message reference: " + messageId + " with value: " + messageRef.getValue());
                    
                    messagesRef.child(messageId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot messageSnapshot) {
                            Message message = messageSnapshot.getValue(Message.class);
                            if (message != null) {
                                // Always set the message ID
                                message.setId(messageId);
                                
                                if (isNotes) {
                                // For notes, we don't need to set sender/recipient
                                // Set isNote after Firebase deserialization to ensure it's not overridden
                                message.setNote(true);
                                Log.d("MessageListFragment", "Set isNote=true for message: " + messageId);
                            } else if (isInbox) {
                                // For inbox, set recipient ID
                                message.setRecipientId(currentUserId);
                                message.setNote(false);
                                Log.d("MessageListFragment", "Added received message from: " + message.getSenderId());
                            } else {
                                // For sent, set sender ID
                                message.setSenderId(currentUserId);
                                message.setNote(false);
                                Log.d("MessageListFragment", "Added sent message to: " + message.getRecipientId());
                            }
                                allMessages.add(message);
                                Log.d("MessageListFragment", "Added message with ID: " + messageId);
                            }
                            
                            // Check if we've loaded all messages
                            if (++loadedCount[0] >= totalMessages) {
                                checkAllQueriesCompleted(++queriesCompleted[0], allMessages);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Log.e("MessageListFragment", "Error loading message " + messageId + ": " + databaseError.getMessage() + 
                                ", Details: " + databaseError.getDetails() + 
                                ", Code: " + databaseError.getCode(), databaseError.toException());
                            if (++loadedCount[0] >= totalMessages) {
                                checkAllQueriesCompleted(++queriesCompleted[0], allMessages);
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Cancel the timeout since we got an error
                if (timeoutRunnable != null) {
                    timeoutHandler.removeCallbacks(timeoutRunnable);
                    timeoutRunnable = null;
                }
                
                Log.e("MessageListFragment", "Error loading sent messages refs: ", databaseError.toException());
                checkAllQueriesCompleted(++queriesCompleted[0], allMessages);
            }
        });
        
        // For notes or regular messages, we only need to load from one place
        // The tabRef is already set up at the beginning of the method
        // No need for separate receivedRef handling
    }
    
    private void checkAllQueriesCompleted(int completedQueries, List<Message> messages) {
        // For notes or regular messages, we only have one query to wait for
        if (completedQueries == 1) {
            final int messageCount = messages != null ? messages.size() : 0;
            Log.d("MessageListFragment", "Loaded " + messageCount + " messages for " + 
                (isNotes ? "notes" : (isInbox ? "inbox" : "outbox")));
            
            // Update UI on the main thread
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    try {
                        // Clear existing messages
                        if (this.messages != null) {
                            this.messages.clear();
                            
                            // Add new messages if available
                            if (messages != null && !messages.isEmpty()) {
                                this.messages.addAll(messages);
                                Log.d("MessageListFragment", "Added " + messages.size() + " messages to the list");
                            }
                        }
                        
                        // Update the adapter
                        if (adapter != null) {
                            adapter.updateMessages(new ArrayList<>(this.messages));
                        } else {
                            Log.e("MessageListFragment", "Adapter is null, cannot update messages");
                        }
                        
                        // Update UI based on message count
                        if (this.messages.isEmpty()) {
                            Log.d("MessageListFragment", "No messages found, showing empty state");
                            updateUIForEmptyState();
                        } else {
                            Log.d("MessageListFragment", "Showing " + this.messages.size() + " messages");
                            if (binding != null) {
                                binding.progressBar.setVisibility(View.GONE);
                                binding.recyclerView.setVisibility(View.VISIBLE);
                                binding.textEmpty.setVisibility(View.GONE);
                            }
                        }
                    } catch (Exception e) {
                        Log.e("MessageListFragment", "Error updating UI with messages", e);
                        updateUIForEmptyState();
                    } finally {
                        // Always hide loading indicators
                        if (binding != null) {
                            binding.swipeRefreshLayout.setRefreshing(false);
                            binding.progressBar.setVisibility(View.GONE);
                        }
                    }
                });
            } else {
                Log.e("MessageListFragment", "Activity is null, cannot update UI");
            }
        }
    }


    private void setupRecyclerView() {
        if (binding == null || binding.recyclerView == null) return;
        
        recyclerView = binding.recyclerView;
        
        // Initialize adapter if not already done
        if (adapter == null) {
            // Create adapter with message action listener and inbox/outbox flag
            adapter = new MessageAdapter(new MessageAdapter.OnMessageActionListener() {
                @Override
                public void onMessageSelected(Message message) {
                    if (isMultiSelectMode) {
                        // Toggle selection in multi-select mode
                        adapter.toggleSelection(message.getId());
                    } else {
                        // Open message details in normal mode
                        if (getActivity() != null && message != null) {
                            String messageId = message.getId();
                            if (messageId != null && !messageId.isEmpty()) {
                                Log.d("MessageListFragment", "Opening message with ID: " + messageId);
                                Intent intent = new Intent(requireContext(), MessageDetailActivity.class);
                                intent.putExtra("message_id", messageId);
                                startActivity(intent);
                            } else {
                                Log.e("MessageListFragment", "Cannot open message: message ID is null or empty");
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "Error: Could not open message", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }
                }

                @Override
                public void onMessageLongClicked(Message message) {
                    // Enter multi-select mode on long click
                    if (!isMultiSelectMode) {
                        isMultiSelectMode = true;
                        if (trashMenuItem != null) {
                            trashMenuItem.setVisible(true);
                        }
                    }
                    // Toggle selection of the clicked item
                    adapter.toggleSelection(message.getId());
                    // Show the action mode if not already shown
                    if (getActivity() != null && getActivity() instanceof AppCompatActivity) {
                        ((AppCompatActivity) getActivity()).startSupportActionMode(actionModeCallback);
                    }
                }
                
                @Override
                public void onSelectionChanged(int selectedCount) {
                    // Update action mode title with selected count
                    if (getActivity() != null && getActivity() instanceof AppCompatActivity) {
                        ActionMode mode = null;
                        View customView = ((AppCompatActivity) getActivity()).getSupportActionBar().getCustomView();
                        if (customView != null) {
                            mode = (ActionMode) customView.getTag();
                        }
                        if (mode != null) {
                            mode.setTitle(String.valueOf(selectedCount));
                            mode.invalidate();
                        }
                    }
                }
            }, isInbox);
        }

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        
        // Set multi-select mode if needed
        if (adapter != null) {
            adapter.setMultiSelectMode(isMultiSelectMode);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("MessageListFragment", "onResume() called, isInbox: " + isInbox + ", isNotes: " + isNotes);
        
        // Update toolbar title
        updateToolbarTitle();
        
        // Refresh messages when fragment becomes visible
        if (isAdded() && getUserVisibleHint()) {
            Log.d("MessageListFragment", "Fragment is visible, refreshing messages");
            loadMessagesDirectly();
        }
    }
    
    /**
     * Updates the UI based on the current tab (inbox/outbox/notes)
     */
    private void updateUIForCurrentTab() {
        if (getActivity() == null || binding == null) return;
        
        // Update FAB icon based on tab
        binding.fabCompose.setImageResource(isNotes ? R.drawable.ic_note_add : R.drawable.ic_add);

        // Update empty state message based on tab
        updateUIForEmptyState();
        
        // Update toolbar title
        updateToolbarTitle();
    }
    
    /**
     * Updates the toolbar title based on the current tab
     */
    private void updateToolbarTitle() {
        if (getActivity() != null && getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity.getSupportActionBar() != null) {
                int titleResId = isNotes ? R.string.label_notes : 
                                     isInbox ? R.string.label_inbox : 
                                     R.string.label_sent;
                activity.getSupportActionBar().setTitle(titleResId);
            }
        }
    }

    /**
     * Updates the messages displayed in the fragment
     * @param messages List of messages to display (can be null or empty)
     */
    public void updateMessages(List<Message> messages) {
        final String TAG = "MessageListFragment";
        final int messageCount = messages != null ? messages.size() : 0;
        Log.d(TAG, String.format("updateMessages() called with %d messages, isInbox: %b", messageCount, isInbox));
        
        // Check if we're in a valid state
        if (getActivity() == null || getActivity().isFinishing() || getActivity().isDestroyed()) {
            Log.w(TAG, "Activity is not in a valid state, ignoring update");
            return;
        }
        
        if (binding == null) {
            Log.w(TAG, "View binding is null, view might be destroyed");
            return;
        }
        
        try {
            // Clear existing messages
            this.messages.clear();
            
            // Process new messages if available
            if (messageCount > 0) {
                // Create a defensive copy to avoid potential concurrency issues
                List<Message> newMessages = new ArrayList<>(messages);
                this.messages.addAll(newMessages);
                
                // Log first few messages for debugging
                int maxMessagesToLog = Math.min(3, newMessages.size());
                for (int i = 0; i < maxMessagesToLog; i++) {
                    Message msg = newMessages.get(i);
                    Log.d(TAG, String.format("Message %d: id=%s, subject=%s, from=%s, to=%s", 
                            i+1, 
                            msg.getId(), 
                            msg.getSubject(), 
                            msg.getSenderId(), 
                            msg.getRecipientId()));
                }
                
                Log.d(TAG, String.format("Added %d messages to the list", newMessages.size()));
            } else {
                Log.d(TAG, "No messages to display");
            }
            
            // Update UI on the main thread
            getActivity().runOnUiThread(this::updateUIState);
            
        } catch (Exception e) {
            Log.e(TAG, "Error in updateMessages", e);
            
            // Update UI to show error state
            if (getActivity() != null && !getActivity().isFinishing()) {
                getActivity().runOnUiThread(() -> {
                    if (binding != null) {
                        try {
                            binding.progressBar.setVisibility(View.GONE);
                            binding.recyclerView.setVisibility(View.GONE);
                            binding.textEmpty.setVisibility(View.VISIBLE);
                            binding.textEmpty.setText(R.string.error_loading_messages);
                            
                            // Ensure swipe refresh is stopped
                            if (binding.swipeRefreshLayout.isRefreshing()) {
                                binding.swipeRefreshLayout.setRefreshing(false);
                            }
                        } catch (Exception uiEx) {
                            Log.e(TAG, "Error updating error UI state", uiEx);
                        }
                    }
                });
            }
        }
    }
    
    /**
     * Updates the UI state based on the current messages
     */
    private void updateUIState() {
        if (getActivity() == null || binding == null) {
            Log.w("MessageListFragment", "Cannot update UI - activity or binding is null");
            return;
        }
        
        try {
            Log.d("MessageListFragment", "Updating UI state, message count: " + messages.size());
            
            // Initialize adapter if needed
            if (adapter == null) {
                Log.d("MessageListFragment", "Initializing adapter");
                setupRecyclerView();
            }
            
            // Update the adapter with current messages
            if (adapter != null) {
                Log.d("MessageListFragment", "Updating adapter with " + messages.size() + " messages");
                adapter.updateMessages(new ArrayList<>(messages)); // Pass a copy to avoid concurrent modification
            } else {
                Log.e("MessageListFragment", "Adapter is null after setup");
            }
            
            // Update visibility based on message count
            if (messages.isEmpty()) {
                Log.d("MessageListFragment", "No messages, showing empty state");
                updateUIForEmptyState();
            } else {
                Log.d("MessageListFragment", messages.size() + " messages, showing list");
                binding.progressBar.setVisibility(View.GONE);
                binding.recyclerView.setVisibility(View.VISIBLE);
                binding.textEmpty.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e("MessageListFragment", "Error updating UI state", e);
        } finally {
            // Always stop refresh animation if it's active
            if (binding != null && binding.swipeRefreshLayout.isRefreshing()) {
                binding.swipeRefreshLayout.setRefreshing(false);
            }
        }
    }
    

    
    /**
     * Shows or hides the loading indicator
     * @param show true to show the loading indicator, false to hide it
     */
    private void showLoadingIndicator(boolean show) {
        if (getActivity() == null || getActivity().isFinishing() || getActivity().isDestroyed()) {
            Log.w("MessageListFragment", "Cannot update loading indicator - activity is not valid");
            return;
        }
        
        getActivity().runOnUiThread(() -> {
            try {
                if (binding == null) {
                    Log.w("MessageListFragment", "Cannot update loading indicator - binding is null");
                    return;
                }
                
                if (show) {
                    Log.d("MessageListFragment", "Showing loading indicator");
                    binding.progressBar.setVisibility(View.VISIBLE);
                    binding.recyclerView.setVisibility(View.GONE);
                    binding.textEmpty.setVisibility(View.GONE);
                    
                    // Ensure swipe refresh is not showing at the same time
                    if (binding.swipeRefreshLayout.isRefreshing()) {
                        binding.swipeRefreshLayout.setRefreshing(false);
                    }
                } else {
                    Log.d("MessageListFragment", "Hiding loading indicator");
                    binding.progressBar.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                Log.e("MessageListFragment", "Error updating loading indicator", e);
                try {
                    // Try to at least hide the progress bar if there's an error
                    if (binding != null && binding.progressBar != null) {
                        binding.progressBar.setVisibility(View.GONE);
                    }
                } catch (Exception ex) {
                    Log.e("MessageListFragment", "Error hiding progress bar after error", ex);
                }
            }
        });
    }
    
    /**
     * Updates the UI to show empty state when there are no messages
     */
    private void updateUIForEmptyState() {
        if (getActivity() == null) {
            Log.w("MessageListFragment", "Cannot show empty state - activity is null");
            return;
        }
        
        // Run on UI thread to ensure thread safety
        getActivity().runOnUiThread(() -> {
            if (binding == null) {
                Log.w("MessageListFragment", "Cannot show empty state - binding is null");
                return;
            }
            
            String tabName = isNotes ? "notes" : (isInbox ? "inbox" : "outbox");
            Log.d("MessageListFragment", "Showing empty state for " + tabName);
            
            try {
                // Hide loading indicators
                binding.progressBar.setVisibility(View.GONE);
                if (binding.swipeRefreshLayout.isRefreshing()) {
                    binding.swipeRefreshLayout.setRefreshing(false);
                }
                
                // Hide recycler view and show empty state
                binding.recyclerView.setVisibility(View.GONE);
                binding.textEmpty.setVisibility(View.VISIBLE);
                
                // Set appropriate empty state message
                int messageResId;
                if (isNotes) {
                    messageResId = R.string.no_notes;
                } else {
                    messageResId = isInbox ? R.string.no_messages_inbox : R.string.no_messages_outbox;
                }
                
                String message = getString(messageResId);
                binding.textEmpty.setText(message);
                binding.textEmpty.bringToFront();
                
                Log.d("MessageListFragment", "Empty state message set: " + message);
                
            } catch (Exception e) {
                Log.e("MessageListFragment", "Error in updateUIForEmptyState", e);
                try {
                    // Try to show a generic error message
                    if (binding.textEmpty != null) {
                        binding.textEmpty.setText(R.string.error_loading_messages);
                    }
                } catch (Exception ex) {
                    Log.e("MessageListFragment", "Error setting error message", ex);
                }
            } finally {
                // Ensure loading indicators are always hidden
                if (binding != null) {
                    binding.progressBar.setVisibility(View.GONE);
                    if (binding.swipeRefreshLayout.isRefreshing()) {
                        binding.swipeRefreshLayout.setRefreshing(false);
                    }
                }
            }
        });
    }
    
    private void loadMessages() {
        // Show loading state
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (binding != null) {
                    binding.progressBar.setVisibility(View.VISIBLE);
                    binding.recyclerView.setVisibility(View.GONE);
                    binding.textEmpty.setVisibility(View.GONE);
                }
            });
        }
        
        // Request messages from parent activity
        if (getActivity() instanceof MessageListActivity) {
            ((MessageListActivity) getActivity()).loadMessages();
        }
    }
    
    private void updateMessagesList(List<Message> newMessages) {
        updateMessages(newMessages);
    }
    


    @Override
    public void onDestroyView() {
        // Cancel any pending timeouts
        if (timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
            timeoutRunnable = null;
        }
        
        // Clear any Firebase listeners
        if (binding != null && binding.swipeRefreshLayout != null) {
            binding.swipeRefreshLayout.setRefreshing(false);
        }
        
        // Clear the binding when the view is destroyed
        binding = null;
        
        super.onDestroyView();
    }
    
    // setupRecyclerView is now implemented above with full functionality
    
    private void exitMultiSelectMode() {
        isMultiSelectMode = false;
        if (adapter != null) {
            adapter.setMultiSelectMode(false);
            adapter.clearSelections();
        }
        if (trashMenuItem != null) {
            trashMenuItem.setVisible(false);
        }
    }
    
    @Override
    public void onStop() {
        super.onStop();
        // Exit multi-select mode when the fragment is stopped
        if (isMultiSelectMode) {
            exitMultiSelectMode();
        }
    }
    
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_message_list, menu);
        
        // Get reference to the trash menu item
        trashMenuItem = menu.findItem(R.id.action_delete);
        if (trashMenuItem != null) {
            // Initially hide the trash icon
            trashMenuItem.setVisible(false);
        }
        
        super.onCreateOptionsMenu(menu, inflater);
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_delete) {
            // Show delete confirmation dialog
            showDeleteConfirmationDialog();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    private void showDeleteConfirmationDialog() {
        if (getContext() == null || !isAdded() || adapter == null) {
            return;
        }
        
        int selectedCount = adapter.getSelectedCount();
        if (selectedCount == 0) {
            return;
        }
        
        String message = getResources().getQuantityString(
            R.plurals.confirm_delete_messages, selectedCount, selectedCount);
            
        new AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_messages)
            .setMessage(message)
            .setPositiveButton(R.string.delete, (dialog, which) -> deleteSelectedMessages())
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }
    
    private void deleteSelectedMessages() {
        if (adapter == null || databaseHelper == null) {
            return;
        }
        
        List<String> selectedIds = adapter.getSelectedMessageIds();
        if (selectedIds.isEmpty()) {
            return;
        }
        
        // Show loading indicator
        showLoadingIndicator(true);
        
        // Delete messages using DatabaseHelper
        databaseHelper.deleteMessages(selectedIds, new DatabaseHelper.MessageOperationCallback() {
            @Override
            public void onSuccess() {
                // Exit multi-select mode and refresh the list
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoadingIndicator(false);
                        exitMultiSelectMode();
                        loadMessagesDirectly();
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoadingIndicator(false);
                        Toast.makeText(requireContext(), 
                            getString(R.string.error_deleting_messages, error), 
                            Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    
    /**
     * Shows a context menu with actions for the selected message
     * @param message The message to show actions for
     */
    private void showMessageContextMenu(Message message) {
        if (getContext() == null || getActivity() == null) return;
        
        // Create a bottom sheet dialog
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_message_actions, null);
        bottomSheetDialog.setContentView(bottomSheetView);
        
        // Initialize views
        TextView title = bottomSheetView.findViewById(R.id.message_action_title);
        View replyLayout = bottomSheetView.findViewById(R.id.action_reply);
        View forwardLayout = bottomSheetView.findViewById(R.id.action_forward);
        View deleteLayout = bottomSheetView.findViewById(R.id.action_delete);
        View cancelLayout = bottomSheetView.findViewById(R.id.action_cancel);
        
        // Set message subject as title
        if (message.getSubject() != null && !message.getSubject().isEmpty()) {
            title.setText(message.getSubject());
        } else {
            title.setText(R.string.no_subject);
        }
        
        // Set up click listeners
        replyLayout.setOnClickListener(v -> {
            // Handle reply action
            Intent intent = new Intent(getContext(), ComposeMessageActivity.class);
            intent.putExtra(ComposeMessageActivity.EXTRA_REPLY_TO, message.getSenderEmail());
            intent.putExtra(ComposeMessageActivity.EXTRA_SUBJECT, 
                "Re: " + (message.getSubject() != null ? message.getSubject() : ""));
            startActivity(intent);
            bottomSheetDialog.dismiss();
        });
        
        forwardLayout.setOnClickListener(v -> {
            // Handle forward action
            Intent intent = new Intent(getContext(), ComposeMessageActivity.class);
            intent.putExtra(ComposeMessageActivity.EXTRA_FORWARD_MESSAGE_ID, message.getId());
            intent.putExtra(ComposeMessageActivity.EXTRA_SUBJECT, 
                "Fwd: " + (message.getSubject() != null ? message.getSubject() : ""));
            startActivity(intent);
            bottomSheetDialog.dismiss();
        });
        
        deleteLayout.setOnClickListener(v -> {
            // Handle delete action
            showDeleteConfirmationDialog(message);
            bottomSheetDialog.dismiss();
        });
        
        cancelLayout.setOnClickListener(v -> bottomSheetDialog.dismiss());
        
        // Show the bottom sheet
        bottomSheetDialog.show();
    }
    
    /**
     * Shows a confirmation dialog before deleting a message
     * @param message The message to be deleted
     */
    private void showDeleteConfirmationDialog(Message message) {
        if (getContext() == null || getActivity() == null) return;
        
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_message_title)
                .setMessage(R.string.delete_message_confirmation)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> deleteMessage(message))
                .setNegativeButton(android.R.string.no, null)
                .show();
    }
    
    /**
     * Deletes the specified message from the database
     * @param message The message to delete
     */
    private void deleteMessage(Message message) {
        if (getContext() == null || getActivity() == null || databaseHelper == null) return;
        
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        
        String userId = currentUser.getUid();
        String messageType;
        if (isNotes) {
            messageType = "notes";
        } else {
            messageType = isInbox ? "received" : "sent";
        }
        
        DatabaseReference messageRef = databaseHelper.getDatabaseReference()
                .child("user-messages")
                .child(userId)
                .child(messageType)
                .child(message.getId());
                
        messageRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    // Show success message
                    if (getContext() != null) {
                        Toast.makeText(getContext(), R.string.message_deleted, Toast.LENGTH_SHORT).show();
                    }
                    // Reload messages
                    loadMessages();
                })
                .addOnFailureListener(e -> {
                    Log.e("MessageListFragment", "Error deleting message", e);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), R.string.error_deleting_message, Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && isResumed()) {
            // Update the tab state based on the current position
            if (getArguments() != null) {
                int newPosition = getArguments().getInt(ARG_POSITION, 0);
                boolean newIsInbox = (newPosition == 0);
                boolean newIsNotes = (newPosition == 2);
                
                // Only refresh if the tab has changed
                if (newIsNotes != isNotes || newIsInbox != isInbox) {
                    position = newPosition;
                    isInbox = newIsInbox;
                    isNotes = newIsNotes;
                    
                    // Clear the current messages
                    messages.clear();
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                    
                    // Show loading indicator
                    showLoadingIndicator(true);
                    
                    // Update the tab state in DatabaseHelper
                    if (databaseHelper != null) {
                        databaseHelper.setTabState(isNotes, isInbox);
                    }
                    
                    // Load messages for the new tab
                    Log.d("MessageListFragment", "Tab changed - Loading messages for tab: " + 
                            (isNotes ? "Notes" : (isInbox ? "Inbox" : "Outbox")));
                    loadMessagesDirectly();
                }
            }
            
            // Always update the toolbar title
            updateToolbarTitle();
        }
    }
    

}
