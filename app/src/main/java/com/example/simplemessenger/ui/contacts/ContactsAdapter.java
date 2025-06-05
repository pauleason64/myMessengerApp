package com.example.simplemessenger.ui.contacts;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.simplemessenger.R;
import com.example.simplemessenger.data.model.User;

import java.util.List;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactViewHolder> {

    public interface OnContactActionListener {
        void onContactClick(User user);
        void onDeleteClick(User user);
    }

    private final List<User> contactsList;
    private final OnContactActionListener listener;

    public ContactsAdapter(List<User> contactsList, OnContactActionListener listener) {
        this.contactsList = contactsList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        User user = contactsList.get(position);
        holder.bind(user, listener);
    }

    @Override
    public int getItemCount() {
        return contactsList.size();
    }

    public void removeContact(User user) {
        int position = contactsList.indexOf(user);
        if (position != -1) {
            contactsList.remove(position);
            notifyItemRemoved(position);
        }
    }

    static class ContactViewHolder extends RecyclerView.ViewHolder {
        private final TextView contactName;
        private final TextView contactEmail;
        private final ImageButton deleteButton;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            contactName = itemView.findViewById(R.id.contactName);
            contactEmail = itemView.findViewById(R.id.contactEmail);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }

        public void bind(User user, OnContactActionListener listener) {
            contactName.setText(user.getDisplayName() != null ? user.getDisplayName() : "No name");
            contactEmail.setText(user.getEmail());
            
            // Set click listener for the entire item
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onContactClick(user);
                }
            });
            
            // Set click listener for the delete button
            deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(user);
                }
            });
        }
    }
}
