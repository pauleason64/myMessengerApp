package com.example.simplemessenger.ui.messaging;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.simplemessenger.R;
import com.example.simplemessenger.SimpleMessengerApp;
import com.example.simplemessenger.data.model.Message;
import com.example.simplemessenger.databinding.ActivityMessageListBinding;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MessageListActivity extends AppCompatActivity {

    private ActivityMessageListBinding binding;
    private DatabaseReference database;
    private FirebaseAuth mAuth;
    private MessageAdapter adapter;
    private ActionMode actionMode;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMessageListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase instances
        database = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        // Set up the toolbar
        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
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

        // Set up FAB
        binding.fab.setOnClickListener(view -> {
            startActivity(new Intent(this, ComposeMessageActivity.class));
        });

        // Load messages
        loadMessages();
    }


    private void loadMessages() {
        if (currentUserId == null) {
            // User not authenticated
            binding.swipeRefreshLayout.setRefreshing(false);
            return;
        }

        // Show loading indicator
        binding.swipeRefreshLayout.setRefreshing(true);

        // Query to get messages where current user is either sender or recipient
        Query query = database.child("user-messages").child(currentUserId)
                .child("received") // or "sent" depending on your needs
                .orderByChild("timestamp");

        // Add a single value event listener to fetch the data once
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Message> messages = new ArrayList<>();
                
                // Iterate through the data and convert to Message objects
                for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                    // Get the message ID
                    String messageId = messageSnapshot.getKey();
                    // Fetch the full message from the messages node
                    database.child("messages").child(messageId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Message message = snapshot.getValue(Message.class);
                            if (message != null) {
                                message.setId(messageId);
                                messages.add(message);
                                // Update the adapter with new data
                                if (adapter == null) {
                                    // Initialize adapter
                                    adapter = new MessageAdapter();
                                    binding.recyclerView.setAdapter(adapter);
                                } else {
                                    adapter.updateMessages(messages);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("MessageListActivity", "Error loading message: " + error.getMessage());
                        }
                    });
                }
                
                // Update UI
                binding.swipeRefreshLayout.setRefreshing(false);
                if (messages.isEmpty()) {
                    binding.textEmpty.setVisibility(View.VISIBLE);
                    binding.recyclerView.setVisibility(View.GONE);
                } else {
                    binding.textEmpty.setVisibility(View.GONE);
                    binding.recyclerView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("MessageListActivity", "Error loading messages: " + databaseError.getMessage());
                binding.swipeRefreshLayout.setRefreshing(false);
                binding.textEmpty.setVisibility(View.VISIBLE);
                binding.recyclerView.setVisibility(View.GONE);
            }
        });
    }

    // Custom Adapter for messages
    private class MessageAdapter extends RecyclerView.Adapter<MessageViewHolder> {
        private List<Message> messages;
        private ValueEventListener valueEventListener;
        private Query query;

        public MessageAdapter() {
            this.messages = new ArrayList<>();
            startListening();
        }

        public void startListening() {
            if (valueEventListener != null) {
                return; // Already listening
            }

            query = database.child("user-messages").child(currentUserId)
                    .child("received") // or "sent" depending on your needs
                    .orderByChild("timestamp");

            valueEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    List<Message> newMessages = new ArrayList<>();
                    for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                        String messageId = messageSnapshot.getKey();
                        database.child("messages").child(messageId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                Message message = snapshot.getValue(Message.class);
                                if (message != null) {
                                    message.setId(messageId);
                                    newMessages.add(message);
                                    updateMessages(newMessages);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e("MessageAdapter", "Error loading message: " + error.getMessage());
                            }
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("MessageAdapter", "Error loading messages: " + databaseError.getMessage());
                }
            };
            
            query.addValueEventListener(valueEventListener);
        }

        public void stopListening() {
            if (valueEventListener != null && query != null) {
                query.removeEventListener(valueEventListener);
                valueEventListener = null;
            }
        }

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
            View view = getLayoutInflater().inflate(R.layout.item_message, parent, false);
            return new MessageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
            Message message = messages.get(position);
            holder.bind(message);
            
            // Set click listener for the item
            holder.itemView.setOnClickListener(v -> {
                // Open message detail
                Intent intent = new Intent(MessageListActivity.this, MessageDetailActivity.class);
                intent.putExtra("message_id", message.getId());
                startActivity(intent);
            });
            
            // Set long click listener for item selection
            holder.itemView.setOnLongClickListener(v -> {
                if (actionMode != null) {
                    return false;
                }
                
                // Start the CAB using the ActionMode.Callback defined above
                actionMode = startSupportActionMode(actionModeCallback);
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }
    }
    
    // ViewHolder for messages
    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView textSender;
        private final TextView textTime;
        private final TextView textSubject;
        private final TextView textPreview;
        private final View imageReminder;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textSender = itemView.findViewById(R.id.text_sender);
            textTime = itemView.findViewById(R.id.text_time);
            textSubject = itemView.findViewById(R.id.text_subject);
            textPreview = itemView.findViewById(R.id.text_preview);
            imageReminder = itemView.findViewById(R.id.image_reminder);
        }

        public void bind(Message message) {
            // Determine if current user is the sender or recipient
            boolean isSender = message.getSenderId() != null && 
                    message.getSenderId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid());
            
            // Set sender/recipient text
            textSender.setText(isSender ? message.getRecipientEmail() : message.getSenderEmail());
            
            // Format and set time
            long timestamp = message.getTimestamp();
            if (timestamp > 0) {
                // Simple time formatting - in a real app, you'd use DateUtils or similar
                String timeStr = android.text.format.DateFormat.getTimeFormat(itemView.getContext())
                        .format(new java.util.Date(timestamp));
                textTime.setText(timeStr);
            } else {
                textTime.setText("");
            }
            
            // Set subject
            textSubject.setText(message.getSubject());
            
            // Set message preview
            textPreview.setText(message.getMessage());
            
            // Show reminder icon if message has a reminder
            imageReminder.setVisibility(message.isHasReminder() ? View.VISIBLE : View.GONE);
        }
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
    protected void onStart() {
        super.onStart();
        if (adapter == null) {
            adapter = new MessageAdapter();
            binding.recyclerView.setAdapter(adapter);
        } else {
            adapter.startListening();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
