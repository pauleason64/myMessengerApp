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
    private ContactsAdapter adapter;
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
        adapter = new ContactsAdapter(contacts, user -> {
            // Handle contact click
            if (getActivity() != null) {
                Toast.makeText(getContext(), "Open chat with " + user.getDisplayName(), Toast.LENGTH_SHORT).show();
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
        updateContactsList(contacts);
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
