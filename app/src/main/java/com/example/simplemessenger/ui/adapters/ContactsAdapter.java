package com.example.SImpleMessenger.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.SImpleMessenger.R;
import com.example.SImpleMessenger.data.ContactsManager;
import com.example.SImpleMessenger.data.model.User;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactViewHolder> {
    private final List<User> contacts;
    private final OnContactClickListener listener;

    public interface OnContactClickListener {
        void onContactClick(User contact);
    }


    public ContactsAdapter(List<User> contacts, OnContactClickListener listener) {
        this.contacts = contacts;
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
        User contact = contacts.get(position);
        holder.bind(contact, listener);
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    public void updateContacts(List<User> newContacts) {
        contacts.clear();
        contacts.addAll(newContacts);
        notifyDataSetChanged();
    }

    static class ContactViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameText;
        private final TextView emailText;
        private final ImageButton deleteButton;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.contactName);
            emailText = itemView.findViewById(R.id.contactEmail);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }

        public void bind(User contact, OnContactClickListener listener) {
            nameText.setText(contact.getDisplayName());
            emailText.setText(contact.getEmail());

            itemView.setOnClickListener(v -> listener.onContactClick(contact));
            
            deleteButton.setOnClickListener(v -> {
                String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                ContactsManager.getInstance().removeContact(contact.getUid())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(itemView.getContext(),
                            "Contact removed", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(itemView.getContext(),
                            "Failed to remove contact: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    });
            });
        }
    }
}
