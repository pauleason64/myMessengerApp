package com.example.simplemessenger;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.simplemessenger.data.model.Contact;
import com.example.simplemessenger.ui.messaging.MessageListActivity;
import com.example.simplemessenger.data.ContactsManager;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ContactsManager contactsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize Firebase and other components
        SimpleMessengerApp app = (SimpleMessengerApp) getApplication();
        contactsManager = ContactsManager.getInstance();
        
        // Check if we need to load contacts first
        boolean loadContactsFirst = getIntent().getBooleanExtra("load_contacts_first", false);
        
        if (loadContactsFirst) {
            loadContactsAndShowMessages();
        } else {
            // Show messages directly
            showMessages();
//            Intent intent = new Intent(this, MessageListActivity.class);
//            startActivity(intent);
//            finish();
        }
    }

    private void loadContactsAndShowMessages() {
        // Set up contact listener
        contactsManager.setLoadListener(new ContactsManager.ContactsLoadListener() {
            @Override
            public void onContactsLoaded(List<Contact> contacts) {
                Log.d("MainActivity", "Contacts loaded: " + contacts.size() + " contacts");
                showMessages();
            }

            @Override
            public void onContactAdded(Contact contact) {
                Log.d("MainActivity", "Contact added: " + contact.getEmailAddress());
            }

            @Override
            public void onContactRemoved(Contact contact) {
                Log.d("MainActivity", "Contact removed: " + contact.getEmailAddress());
            }

            @Override
            public void onError(String error) {
                Log.e("MainActivity", "Error loading contacts: " + error);
                // Show messages even if contacts fail to load
                showMessages();
            }
        });
        
        // Load contacts
        contactsManager.initializeContacts();
    }

    private void showMessages() {
        Intent intent = new Intent(this, MessageListActivity.class);
        startActivity(intent);
        finish();
    }

    // Note: Message loading is handled by MessageListActivity
}
