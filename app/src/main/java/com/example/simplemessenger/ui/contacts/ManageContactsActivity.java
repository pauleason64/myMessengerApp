package com.example.SImpleMessenger.ui.contacts;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.SImpleMessenger.R;
import com.example.SImpleMessenger.data.model.User;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.example.SImpleMessenger.util.FirebaseFactory;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ManageContactsActivity extends AppCompatActivity {

    private RecyclerView contactsRecyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;
    private ContactsAdapter contactsAdapter;
    private List<User> contactsList = new ArrayList<>();
    private DatabaseReference contactsRef;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_contacts);
        
        // Initialize Firebase
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        contactsRef = FirebaseFactory.getDatabase().getReference("users");
        
        // Initialize the Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // Set up the back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.title_manage_contacts);
        }
        
        // Initialize views
        contactsRecyclerView = findViewById(R.id.contacts_recycler_view);
        progressBar = findViewById(R.id.progress_bar);
        emptyView = findViewById(R.id.empty_view);
        
        // Set up RecyclerView
        contactsAdapter = new ContactsAdapter(contactsList, new ContactsAdapter.OnContactActionListener() {
            @Override
            public void onContactClick(User user) {
                // Handle contact click (e.g., open chat or show details)
                Toast.makeText(ManageContactsActivity.this, 
                    "Selected: " + user.getDisplayName(), 
                    Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeleteClick(User user) {
                // Handle delete button click
                showDeleteConfirmationDialog(user);
            }
        });
        contactsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        contactsRecyclerView.setAdapter(contactsAdapter);
        
        // Set up the FloatingActionButton
        FloatingActionButton fabAddContact = findViewById(R.id.fab_add_contact);
        fabAddContact.setOnClickListener(view -> {
            // TODO: Implement add contact functionality
            Toast.makeText(ManageContactsActivity.this, 
                "Add contact functionality coming soon", Toast.LENGTH_SHORT).show();
        });
        
        // Load contacts
        loadContacts();
    }
    
    private void loadContacts() {
        showLoading(true);
        
        contactsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                contactsList.clear();
                
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null && !user.getUid().equals(currentUserId)) {
                        contactsList.add(user);
                    }
                }
                
                contactsAdapter.notifyDataSetChanged();
                showLoading(false);
                
                // Show empty view if no contacts found
                if (contactsList.isEmpty()) {
                    emptyView.setVisibility(View.VISIBLE);
                    contactsRecyclerView.setVisibility(View.GONE);
                } else {
                    emptyView.setVisibility(View.GONE);
                    contactsRecyclerView.setVisibility(View.VISIBLE);
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                showLoading(false);
                Toast.makeText(ManageContactsActivity.this, 
                    "Failed to load contacts: " + databaseError.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        contactsRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }
    
    private void showDeleteConfirmationDialog(User user) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Contact")
            .setMessage("Are you sure you want to remove " + user.getDisplayName() + " from your contacts?")
            .setPositiveButton(android.R.string.yes, (dialog, which) -> removeContact(user))
            .setNegativeButton(android.R.string.no, null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();
    }
    
    private void removeContact(User user) {
        // In a real app, you would remove the contact from your database here
        // For now, we'll just remove it from the local list and update the UI
        contactsAdapter.removeContact(user);
        
        // Show a message
        Toast.makeText(this, "Contact removed", Toast.LENGTH_SHORT).show();
        
        // Show empty view if the list is now empty
        if (contactsAdapter.getItemCount() == 0) {
            emptyView.setVisibility(View.VISIBLE);
            contactsRecyclerView.setVisibility(View.GONE);
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
