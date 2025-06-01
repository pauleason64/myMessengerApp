package com.example.simplemessenger.ui.messaging.adapter;

import android.annotation.SuppressLint;
import android.icu.text.SimpleDateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.simplemessenger.R;
import com.example.simplemessenger.data.model.Message;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.google.firebase.auth.FirebaseUser;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private List<Message> messages = new ArrayList<>();
    private final OnMessageActionListener actionListener;
    private final boolean isInbox;

    public interface OnMessageActionListener {
        void onMessageSelected(Message message);
        void onMessageLongClicked(Message message);
    }

    public MessageAdapter(OnMessageActionListener actionListener, boolean isInbox) {
        this.actionListener = actionListener;
        this.isInbox = isInbox;
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
        holder.bind(message, isInbox);
        
        // Set click listener for the item
        holder.itemView.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onMessageSelected(message);
            }
        });
        
        // Set long click listener for item selection
        holder.itemView.setOnLongClickListener(v -> {
            if (actionListener != null) {
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
    static class MessageViewHolder extends RecyclerView.ViewHolder {
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

        public void bind(Message message, boolean isInbox) {
            if (message == null) {
                textSender.setText("");
                textTime.setText("");
                textSubject.setText("");
                textPreview.setText("");
                imageReminder.setVisibility(View.GONE);
                return;
            }
            
            // Set the display name based on message type
            String displayName;
            if (message.getIsNote()) {
                // For notes, show "Note" as the sender
                displayName = itemView.getContext().getString(R.string.label_note);
            } else if (isInbox) {
                // In inbox: show sender's email
                displayName = message.getSenderEmail() != null ? message.getSenderEmail() : "Unknown Sender";
            } else {
                // In outbox: show recipient's email
                displayName = message.getRecipientEmail() != null ? message.getRecipientEmail() : "Unknown Recipient";
            }
            textSender.setText(displayName);
            
            // Style the sender text for notes
            if (message.getIsNote()) {
                textSender.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_note_small, 0, 0, 0);
                textSender.setCompoundDrawablePadding(itemView.getContext().getResources()
                        .getDimensionPixelSize(R.dimen.small_padding));
            } else {
                textSender.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
            }
            
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
