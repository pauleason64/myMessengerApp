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
import com.example.simplemessenger.databinding.FragmentContactsListBinding;
import com.example.simplemessenger.ui.adapters.ContactsAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ContactsListFragment extends Fragment implements ContactsManager.ContactsLoadListener {
    private static final String TAG = "ContactsListFragment";

    private FragmentContactsListBinding binding;
    private ContactsManager contactsManager;
    private ContactsAdapter adapter;
    private final List<User> contacts = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        binding = FragmentContactsListBinding.inflate(inflater, container, false);
        return binding.getRoot();
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
                // Open chat with this contact
                // startActivity(ChatActivity.newIntent(getActivity(), user.getUid()));
                Toast.makeText(getContext(), "Open chat with " + user.getDisplayName(), Toast.LENGTH_SHORT).show();
            }
        });
        
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);
    }
    
    @Override
    public void onStart() {
        super.onStart();
        // Register this fragment as a listener for contact updates
        contactsManager.setLoadListener(this);
        // Refresh contacts from cache
        updateContactsList(contactsManager.getCachedContacts());
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
            contacts.clear();
            if (contactsList != null) {
                // Convert Contact objects to User objects for the adapter
                for (Contact contact : contactsList) {
                    User user = new User(contact.getContactId(), contact.getEmailAddress(), contact.getUserName());
                    contacts.add(user);
                }
            }
            adapter.notifyDataSetChanged();
            
            if (contacts.isEmpty()) {
                binding.textNoContacts.setVisibility(View.VISIBLE);
                binding.recyclerView.setVisibility(View.GONE);
            } else {
                binding.textNoContacts.setVisibility(View.GONE);
                binding.recyclerView.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
