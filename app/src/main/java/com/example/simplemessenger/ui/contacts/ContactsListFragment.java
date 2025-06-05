package com.example.simplemessenger.ui.contacts;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.simplemessenger.data.model.Contact;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.simplemessenger.R;
import com.example.simplemessenger.data.ContactsManager;
import com.example.simplemessenger.data.model.User;
import com.example.simplemessenger.R;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.simplemessenger.ui.adapters.ContactsAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ContactsListFragment extends Fragment implements ContactsManager.ContactsLoadListener {
    private static final String TAG = "ContactsListFragment";

    private View rootView;
    private RecyclerView recyclerView;
    private TextView textNoContacts;
    private ProgressBar progressBar;
    private ContactsManager contactsManager;
    private com.example.simplemessenger.ui.contacts.ContactsAdapter adapter;
    private final List<User> contacts = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_contacts_list, container, false);
        recyclerView = rootView.findViewById(R.id.recycler_view);
        textNoContacts = rootView.findViewById(R.id.text_no_contacts);
        progressBar = rootView.findViewById(R.id.progress_bar);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize ContactsManager
        contactsManager = ContactsManager.getInstance();
        
        // Set up RecyclerView
        adapter = new com.example.simplemessenger.ui.contacts.ContactsAdapter(contacts, new com.example.simplemessenger.ui.contacts.ContactsAdapter.OnContactActionListener() {
            @Override
            public void onContactClick(User user) {
                if (getActivity() != null) {
                    Toast.makeText(getContext(), "Open chat with " + user.getDisplayName(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onDeleteClick(User user) {
                if (getActivity() == null) return;
                
                // Show confirmation dialog
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle(R.string.delete_contact)
                    .setMessage(getString(R.string.confirm_delete_contact, user.getDisplayName()))
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                        // User confirmed deletion
                        contactsManager.removeContact(user.getUid())
                            .addOnSuccessListener(aVoid -> {
                                // Contact removed successfully
                                if (getActivity() != null) {
                                    Toast.makeText(getContext(), 
                                        getString(R.string.contact_deleted, user.getDisplayName()), 
                                        Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                // Failed to remove contact
                                if (getActivity() != null) {
                                    Toast.makeText(getContext(), 
                                        getString(R.string.failed_to_delete_contact, e.getMessage()),
                                        Toast.LENGTH_SHORT).show();
                                }
                            });
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            }
        });
        
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        
        // Initialize loading state
        progressBar.setVisibility(View.VISIBLE);
        textNoContacts.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        
        // Set up the load listener first
        contactsManager.setLoadListener(this);
        
        // Then initialize contacts
        contactsManager.initializeContacts();
    }
    
    @Override
    public void onStart() {
        super.onStart();
        // Load any cached contacts
        List<Contact> cachedContacts = contactsManager.getCachedContacts();
        if (cachedContacts != null && !cachedContacts.isEmpty()) {
            updateContactsList(cachedContacts);
        }
    }
    
    @Override
    public void onStop() {
        super.onStop();
        // Unregister listener to prevent memory leaks
        contactsManager.setLoadListener(null);
    }
    
    @Override
    public void onContactsLoaded(List<Contact> contacts) {
        if (getActivity() == null) return;
        
        getActivity().runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            
            // Convert Contact objects to User objects
            List<User> userList = new ArrayList<>();
            for (Contact contact : contacts) {
                if (contact != null) {
                    User user = new User(contact.getContactId(), contact.getEmailAddress(), contact.getUserName());
                    userList.add(user);
                }
            }
            
            this.contacts.clear();
            this.contacts.addAll(userList);
            adapter.notifyDataSetChanged();
            
            if (this.contacts.isEmpty()) {
                textNoContacts.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                textNoContacts.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onContactAdded(Contact contact) {
        // Handle contact added
        if (isAdded() && getContext() != null) {
            updateContactsList(contactsManager.getCachedContacts());
        }
    }
    
    @Override
    public void onContactRemoved(Contact contact) {
        // Handle contact removed
        if (isAdded() && getContext() != null) {
            updateContactsList(contactsManager.getCachedContacts());
        }
    }
    
    @Override
    public void onError(String error) {
        if (isAdded() && getContext() != null) {
            Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateContactsList(Collection<Contact> contactsList) {
        if (getActivity() == null) return;
        
        getActivity().runOnUiThread(() -> {
            // Hide progress bar when we get any response
            progressBar.setVisibility(View.GONE);
            
            contacts.clear();
            if (contactsList != null && !contactsList.isEmpty()) {
                // Convert Contact objects to User objects for the adapter
                for (Contact contact : contactsList) {
                    if (contact != null && contact.getContactId() != null) {
                        User user = new User(contact.getContactId(), 
                                          contact.getEmailAddress(),
                                          contact.getUserName());
                        contacts.add(user);
                    }
                }
                adapter.notifyDataSetChanged();
            }
            
            // Update UI based on whether we have contacts
            if (contacts.isEmpty()) {
                textNoContacts.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
                textNoContacts.setText(R.string.no_contacts_found);
            } else {
                textNoContacts.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recyclerView = null;
        textNoContacts = null;
        progressBar = null;
        rootView = null;
    }
}
