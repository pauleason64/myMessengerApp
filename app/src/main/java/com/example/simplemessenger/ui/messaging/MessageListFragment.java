package com.example.simplemessenger.ui.messaging;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.simplemessenger.R;
import com.example.simplemessenger.databinding.ActivityMessageListBinding;
import com.example.simplemessenger.data.DatabaseHelper;
import com.example.simplemessenger.data.model.Message;
import com.example.simplemessenger.ui.messaging.adapter.MessageAdapter;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MessageListFragment extends Fragment {
    private static final String ARG_SECTION_NUMBER = "section_number";
    private RecyclerView recyclerView;
    private MessageAdapter adapter;
    private List<Message> messages = new ArrayList<>();
    private DatabaseHelper databaseHelper;
    private ValueEventListener messageListener;
    private Query messagesQuery;
    private boolean isAscending = true;
    private boolean isInbox = true;
    private String currentSortField = "timestamp";
    private ActivityMessageListBinding binding;

    public MessageListFragment() {
    }

    public static MessageListFragment newInstance(int sectionNumber) {
        MessageListFragment fragment = new MessageListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseHelper = DatabaseHelper.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                         Bundle savedInstanceState) {
        binding = ActivityMessageListBinding.inflate(inflater, container, false);
        View rootView = binding.getRoot();
        recyclerView = binding.recyclerView;
        if (recyclerView != null) {
            setupRecyclerView();
            setupToolbar();
        }
        return rootView;
    }

    private void setupToolbar() {
        Toolbar toolbar = binding.toolbar;
        if (toolbar != null) {
            ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
            if (((AppCompatActivity) requireActivity()).getSupportActionBar() != null) {
                ((AppCompatActivity) requireActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                ((AppCompatActivity) requireActivity()).getSupportActionBar().setTitle(R.string.title_messages);
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
        }
    }

    private void updateTitle() {
        String title = isInbox ? getString(R.string.label_inbox) : getString(R.string.label_outbox);
        if (binding.textViewTitle != null) {
            binding.textViewTitle.setText(title);
        }
    }

    private void setupRecyclerView() {
        if (recyclerView == null) return;

        adapter = new MessageAdapter(new MessageAdapter.OnMessageActionListener() {
            @Override
            public void onMessageSelected(Message message) {
                // Handle message click
            }

            @Override
            public void onMessageLongClicked(Message message) {
                // Handle message long click
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
    }

    @Override
    public void onStart() {
        super.onStart();
        loadMessages();
    }

    private void loadMessages() {
        if (getContext() == null || databaseHelper == null) {
            Log.e("MessageListFragment", "Context or DatabaseHelper is null");
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e("MessageListFragment", "No current user signed in");
            return;
        }
        Log.d("MessageListFragment", "Loading messages for user: " + currentUser.getUid());
        databaseHelper = DatabaseHelper.getInstance();
        String currentUserId = currentUser.getUid();

        String messageType = isInbox ? "received" : "sent";
        messagesQuery = databaseHelper.getDatabaseReference()
                .child("user-messages")
                .child(currentUserId)
                .child(messageType)
                .orderByChild(currentSortField);

        if (!isAscending) {
            messagesQuery = messagesQuery.limitToLast(100);
        }

        messageListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                messages.clear();
                for (DataSnapshot messageRef : dataSnapshot.getChildren()) {
                    String messageId = messageRef.getKey();
                    databaseHelper.getDatabaseReference()
                            .child("messages")
                            .child(messageId)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @android.annotation.SuppressLint("NotifyDataSetChanged")
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    Message message = snapshot.getValue(Message.class);
                                    if (message != null) {
                                        Log.d("MessageListFragment", "Loaded message: " + message.getSubject());
                                        messages.add(message);
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
                                        adapter.notifyDataSetChanged();
                                    } else {
                                        Log.w("MessageListFragment", "Failed to parse message from snapshot");
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.e("MessageListFragment", "Error loading message: " + error.getMessage(), error.toException());
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("MessageListFragment", "Error loading messages: " + error.getMessage());
            }
        };

        messagesQuery.addValueEventListener(messageListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (messageListener != null && messagesQuery != null) {
            messagesQuery.removeEventListener(messageListener);
        }
    }
}
