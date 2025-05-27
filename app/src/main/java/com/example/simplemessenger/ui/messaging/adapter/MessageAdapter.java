package com.example.simplemessenger.ui.messaging.adapter;

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
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private List<Message> messages = new ArrayList<>();
    private final OnMessageActionListener actionListener;

    public interface OnMessageActionListener {
        void onMessageSelected(Message message);
        void onMessageLongClicked(Message message);
    }

    public MessageAdapter(OnMessageActionListener actionListener) {
        this.actionListener = actionListener;
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
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.bind(message);
        
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

        public void bind(Message message) {
            // Determine if current user is the sender or recipient
            boolean isSender = message.getSenderId() != null && 
                    message.getSenderId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid());
            
            // Set sender/recipient text
            textSender.setText(isSender ? message.getRecipientEmail() : message.getSenderEmail());
            
            // Format and set time
            long timestamp = message.getTimestamp();
            if (timestamp > 0) {
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
}
