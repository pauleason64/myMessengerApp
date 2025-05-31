package com.example.simplemessenger.ui.messaging;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import android.app.AlertDialog;
import android.content.DialogInterface;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.simplemessenger.R;
import com.example.simplemessenger.databinding.FragmentMessageListBinding;
import com.example.simplemessenger.data.DatabaseHelper;
import com.example.simplemessenger.data.model.Message;
import com.example.simplemessenger.ui.messaging.adapter.MessageAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;


public class MessageListFragment extends Fragment {
    private static final String ARG_POSITION = "position";
    private RecyclerView recyclerView;
    private List<Message> messages = new ArrayList<>();
    private boolean isInbox = true;
    private int position = 0;
    private FragmentMessageListBinding binding;
    private MessageAdapter adapter;
    private DatabaseHelper databaseHelper;

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
        
        // Set isInbox based on the position from arguments
        if (getArguments() != null) {
            position = getArguments().getInt(ARG_POSITION, 0);
            isInbox = (position == 0); // 0 for inbox, 1 for outbox
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                         Bundle savedInstanceState) {
        binding = FragmentMessageListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize views
        recyclerView = binding.recyclerView;
        setupRecyclerView();
        
        // Hide the radio group as we're using ViewPager for tab switching
        binding.radioGroup.setVisibility(View.GONE);
        
        // Set up FAB
        binding.fabCompose.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), ComposeMessageActivity.class));
        });
        
        // Set up swipe to refresh with pull-to-refresh
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
        
        // Show loading indicator initially
        showLoadingIndicator(true);
        
        // Load messages directly
        Log.d("MessageListFragment", "Loading messages directly");
        loadMessagesDirectly();
        
        updateToolbarTitle();
    }
    
    private void loadMessagesDirectly() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e("MessageListFragment", "User not authenticated");
            showLoadingIndicator(false);
            binding.swipeRefreshLayout.setRefreshing(false);
            return;
        }
        
        String currentUserId = currentUser.getUid();
        Log.d("MessageListFragment", "Loading messages for user: " + currentUserId);
        
        DatabaseReference userMessagesRef = FirebaseDatabase.getInstance()
                .getReference("user-messages")
                .child(currentUserId);
                
        // Load both sent and received messages
        DatabaseReference sentRef = userMessagesRef.child("sent");
        DatabaseReference receivedRef = userMessagesRef.child("received");
        DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference("messages");
        
        // Use a counter to track both queries
        final int[] queriesCompleted = {0};
        final List<Message> allMessages = new ArrayList<>();
        
        // Load sent messages
        sentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser == null) return;
                
                String currentUserId = currentUser.getUid();
                final int totalMessages = (int) dataSnapshot.getChildrenCount();
                
                if (totalMessages == 0) {
                    checkAllQueriesCompleted(++queriesCompleted[0], allMessages);
                    return;
                }
                
                final int[] loadedCount = {0};
                
                for (DataSnapshot messageRef : dataSnapshot.getChildren()) {
                    String messageId = messageRef.getKey();
                    if (messageId == null) continue;
                    
                    messagesRef.child(messageId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot messageSnapshot) {
                            Message message = messageSnapshot.getValue(Message.class);
                            if (message != null) {
                                message.setSenderId(currentUserId);
                                allMessages.add(message);
                            }
                            
                            // Check if we've loaded all messages
                            if (++loadedCount[0] >= totalMessages) {
                                checkAllQueriesCompleted(++queriesCompleted[0], allMessages);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Log.e("MessageListFragment", "Error loading sent message: " + messageId, databaseError.toException());
                            if (++loadedCount[0] >= totalMessages) {
                                checkAllQueriesCompleted(++queriesCompleted[0], allMessages);
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("MessageListFragment", "Error loading sent messages refs: ", databaseError.toException());
                checkAllQueriesCompleted(++queriesCompleted[0], allMessages);
            }
        });
        
        // Load received messages
        receivedRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser == null) return;
                
                String currentUserId = currentUser.getUid();
                final int totalMessages = (int) dataSnapshot.getChildrenCount();
                
                if (totalMessages == 0) {
                    checkAllQueriesCompleted(++queriesCompleted[0], allMessages);
                    return;
                }
                
                final int[] loadedCount = {0};
                
                for (DataSnapshot messageRef : dataSnapshot.getChildren()) {
                    String messageId = messageRef.getKey();
                    if (messageId == null) continue;
                    
                    messagesRef.child(messageId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot messageSnapshot) {
                            Message message = messageSnapshot.getValue(Message.class);
                            if (message != null) {
                                message.setRecipientId(currentUserId);
                                allMessages.add(message);
                            }
                            
                            // Check if we've loaded all messages
                            if (++loadedCount[0] >= totalMessages) {
                                checkAllQueriesCompleted(++queriesCompleted[0], allMessages);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Log.e("MessageListFragment", "Error loading received message: " + messageId, databaseError.toException());
                            if (++loadedCount[0] >= totalMessages) {
                                checkAllQueriesCompleted(++queriesCompleted[0], allMessages);
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("MessageListFragment", "Error loading received messages refs: ", databaseError.toException());
                checkAllQueriesCompleted(++queriesCompleted[0], allMessages);
            }
        });
    }
    
    private void checkAllQueriesCompleted(int completedQueries, List<Message> messages) {
        // We're waiting for 2 queries (sent and received)
        if (completedQueries == 2) {
            Log.d("MessageListFragment", "Loaded " + messages.size() + " total messages");
            updateMessages(messages);
            binding.swipeRefreshLayout.setRefreshing(false);
            showLoadingIndicator(false);
        }
    }

    private void updateToolbarTitle() {
        // Title is now managed by the parent activity
    }


    private void setupRecyclerView() {
        if (recyclerView == null) return;

        // Create adapter with message action listener and inbox/outbox flag
        adapter = new MessageAdapter(new MessageAdapter.OnMessageActionListener() {
            @Override
            public void onMessageSelected(Message message) {
                // Handle message click
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

            @Override
            public void onMessageLongClicked(Message message) {
                // Handle message long click (e.g., show context menu)
                showMessageContextMenu(message);
            }
        }, isInbox);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("MessageListFragment", "onResume() called, isInbox: " + isInbox);
        
        // Always try to load messages when fragment becomes visible
        if (getActivity() instanceof MessageListActivity) {
            Log.d("MessageListFragment", "Requesting message load from parent activity");
            ((MessageListActivity) getActivity()).loadMessages();
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
     */
    private void showLoadingIndicator(boolean show) {
        if (binding == null) return;
        
        getActivity().runOnUiThread(() -> {
            try {
                if (show) {
                    binding.progressBar.setVisibility(View.VISIBLE);
                    binding.recyclerView.setVisibility(View.GONE);
                    binding.textEmpty.setVisibility(View.GONE);
                } else {
                    binding.progressBar.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                Log.e("MessageListFragment", "Error updating loading indicator", e);
            }
        });
    }
    
    /**
     * Updates the UI to show empty state when there are no messages
     */
    private void updateUIForEmptyState() {
        if (getActivity() == null || binding == null) {
            Log.w("MessageListFragment", "Cannot show empty state - activity or binding is null");
            return;
        }
        
        Log.d("MessageListFragment", "Showing empty state for " + (isInbox ? "inbox" : "outbox"));
        
        try {
            // Update visibility
            binding.progressBar.setVisibility(View.GONE);
            binding.recyclerView.setVisibility(View.GONE);
            binding.textEmpty.setVisibility(View.VISIBLE);
            
            // Set appropriate empty state message
            int messageResId = isInbox ? R.string.no_messages_inbox : R.string.no_messages_outbox;
            String message = getString(messageResId);
            
            if (binding.textEmpty != null) {
                binding.textEmpty.setText(message);
                binding.textEmpty.bringToFront();
            } else {
                Log.e("MessageListFragment", "textEmpty view is null");
            }
            
            Log.d("MessageListFragment", "Empty state message set: " + message);
            
        } catch (Exception e) {
            Log.e("MessageListFragment", "Error in updateUIForEmptyState", e);
        } finally {
            // Always stop refresh animation if it's active
            if (binding != null && binding.swipeRefreshLayout.isRefreshing()) {
                binding.swipeRefreshLayout.setRefreshing(false);
            }
        }
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
        super.onDestroyView();
        // Clear the binding when the view is destroyed
        binding = null;
    }
    
    @Override
    public void onStop() {
        super.onStop();
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
        DatabaseReference messageRef = databaseHelper.getDatabaseReference()
                .child("user-messages")
                .child(userId)
                .child(isInbox ? "received" : "sent")
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
}
