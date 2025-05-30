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
import com.google.firebase.database.DatabaseReference;

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
        
        // Set up swipe to refresh
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            if (getActivity() instanceof MessageListActivity) {
                ((MessageListActivity) getActivity()).loadMessages();
            }
        });
        
        binding.swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        );
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            if (getActivity() instanceof MessageListActivity) {
                ((MessageListActivity) getActivity()).refreshMessages();
            }
        });
        
        updateToolbarTitle();
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
                if (getActivity() != null) {
                    Intent intent = new Intent(requireContext(), MessageDetailActivity.class);
                    intent.putExtra(ComposeMessageActivity.EXTRA_MESSAGE_ID, message.getId());
                    startActivity(intent);
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
    public void onStart() {
        super.onStart();
        // Request initial data load from parent activity
        if (getActivity() instanceof MessageListActivity) {
            ((MessageListActivity) getActivity()).loadMessages();
        }
    }

    /**
     * Updates the messages displayed in the fragment
     * @param messages List of messages to display
     */
    /**
     * Updates the messages displayed in the fragment
     * @param messages List of messages to display
     */
    public void updateMessages(List<Message> messages) {
        if (getActivity() == null || binding == null) return;
        
        Log.d("MessageListFragment", "Updating messages. Count: " + (messages != null ? messages.size() : 0) + ", isInbox: " + isInbox);
        
        // Clear existing messages and add new ones
        this.messages.clear();
        if (messages != null && !messages.isEmpty()) {
            this.messages.addAll(messages);
        }
        
        getActivity().runOnUiThread(() -> {
            if (binding == null) return;
            
            try {
                // Initialize adapter if needed
                if (adapter == null) {
                    setupRecyclerView();
                }
                
                // Update the adapter with new messages
                if (adapter != null) {
                    adapter.updateMessages(this.messages);
                }
                
                // Update UI based on whether we have messages or not
                if (this.messages.isEmpty()) {
                    updateUIForEmptyState();
                } else {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.recyclerView.setVisibility(View.VISIBLE);
                    binding.textEmpty.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                Log.e("MessageListFragment", "Error updating messages", e);
            } finally {
                // Always stop refresh animation if it's active
                if (binding.swipeRefreshLayout.isRefreshing()) {
                    binding.swipeRefreshLayout.setRefreshing(false);
                }
            }
        });
    }
    
    /**
     * Updates the UI to show empty state when there are no messages
     */
    private void updateUIForEmptyState() {
        if (getActivity() == null || binding == null) return;
        
        getActivity().runOnUiThread(() -> {
            try {
                if (binding == null) return;
                
                Log.d("MessageListFragment", "Showing empty state for " + (isInbox ? "inbox" : "outbox"));
                
                // Update visibility
                binding.progressBar.setVisibility(View.GONE);
                binding.recyclerView.setVisibility(View.GONE);
                binding.textEmpty.setVisibility(View.VISIBLE);
                
                // Set appropriate empty state message
                binding.textEmpty.setText(isInbox ? 
                    getString(R.string.no_messages_inbox) : 
                    getString(R.string.no_messages_outbox));
                
                // Make sure the empty text is visible and properly sized
                if (binding.textEmpty.getText().toString().isEmpty()) {
                    binding.textEmpty.setText(isInbox ? 
                        getString(R.string.no_messages_inbox) : 
                        getString(R.string.no_messages_outbox));
                }
                
                // Ensure the empty view is brought to front
                binding.textEmpty.bringToFront();
                
            } catch (Exception e) {
                Log.e("MessageListFragment", "Error in updateUIForEmptyState", e);
            } finally {
                // Always stop refresh animation if it's active
                if (binding != null && binding.swipeRefreshLayout.isRefreshing()) {
                    binding.swipeRefreshLayout.setRefreshing(false);
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
