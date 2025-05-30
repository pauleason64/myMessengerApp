package com.example.simplemessenger.data;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.simplemessenger.util.FirebaseConfig;
import com.example.simplemessenger.util.FirebaseConfigManager;
import com.example.simplemessenger.util.FirebaseFactory;
import com.example.simplemessenger.util.LogWrapper;

import com.example.simplemessenger.data.model.Contact;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContactsManager {
    private static final String TAG = "ContactsManager";
    private static final String CONTACTS_NODE = "user-contacts";
    private static ContactsManager instance;
    private final DatabaseReference databaseReference;
    private final FirebaseAuth auth;
    private final Map<String, Contact> contactsCache = new HashMap<>();
    private final LogWrapper log;
    private ContactsLoadListener loadListener;
    private ContactsLoadListener previousListener;

    public interface ContactsLoadListener {
        void onContactsLoaded(List<Contact> contacts);
        void onContactAdded(Contact contact);
        void onContactRemoved(Contact contact);
        void onError(String error);
    }

    /**
     * Package-private constructor for testing
     */
    ContactsManager(DatabaseReference databaseReference, FirebaseAuth auth) {
        this(databaseReference, auth, new LogWrapper());
    }
    
    /**
     * Package-private constructor for testing with LogWrapper
     */
    ContactsManager(DatabaseReference databaseReference, FirebaseAuth auth, LogWrapper log) {
        this.databaseReference = databaseReference;
        this.auth = auth;
        this.log = log;
    }
    
    private ContactsManager() {
        this.databaseReference = FirebaseFactory.getDatabase().getReference();
        this.auth = FirebaseFactory.getAuth();
        this.log = new LogWrapper();
    }

    public static synchronized ContactsManager getInstance() {
        if (instance == null) {
            instance = new ContactsManager();
        }
        return instance;
    }

    public ContactsLoadListener getLoadListener() {
        return loadListener;
    }

    public void setLoadListener(ContactsLoadListener listener) {
        // Store the previous listener before replacing it
        previousListener = loadListener;
        this.loadListener = listener;
    }

    public void clearPreviousListener() {
        // Clear the previous listener to prevent memory leaks
        previousListener = null;
    }

    private ValueEventListener contactListener;
    private ChildEventListener contactChildListener;

    public void initializeContacts() {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            if (loadListener != null) {
                loadListener.onError("User not authenticated");
            }
            return;
        }

        // First load existing contacts
        contactListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                contactsCache.clear();
                for (DataSnapshot contactSnapshot : dataSnapshot.getChildren()) {
                    Contact contact = contactSnapshot.getValue(Contact.class);
                    if (contact != null) {
                        contact.setId(contactSnapshot.getKey());
                        contactsCache.put(contact.getContactId(), contact);
                        
                        // Notify listener about each contact
                        if (loadListener != null) {
                            loadListener.onContactAdded(contact);
                        }
                    }
                }
                
                // Notify about all contacts loaded
                if (loadListener != null) {
                    loadListener.onContactsLoaded(new ArrayList<>(contactsCache.values()));
                }
                log.d(TAG, "Loaded " + contactsCache.size() + " contacts for user");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (loadListener != null) {
                    loadListener.onError(databaseError.getMessage());
                }
            }
        };

        // Set up listener for real-time updates
        contactChildListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {
                Contact contact = dataSnapshot.getValue(Contact.class);
                if (contact != null) {
                    contact.setId(dataSnapshot.getKey());
                    contactsCache.put(contact.getContactId(), contact);
                    if (loadListener != null) {
                        loadListener.onContactAdded(contact);
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {
                Contact contact = dataSnapshot.getValue(Contact.class);
                if (contact != null) {
                    contact.setId(dataSnapshot.getKey());
                    contactsCache.put(contact.getContactId(), contact);
                    if (loadListener != null) {
                        loadListener.onContactAdded(contact);
                    }
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                Contact contact = dataSnapshot.getValue(Contact.class);
                if (contact != null) {
                    contactsCache.remove(contact.getContactId());
                    if (loadListener != null) {
                        loadListener.onContactRemoved(contact);
                    }
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {
                // Not needed for contacts
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (loadListener != null) {
                    loadListener.onError(databaseError.getMessage());
                }
            }
        };

        // Add both listeners
        databaseReference.child(CONTACTS_NODE).child(currentUserId)
                .addListenerForSingleValueEvent(contactListener);
        databaseReference.child(CONTACTS_NODE).child(currentUserId)
                .addChildEventListener(contactChildListener);
    }

    public void cleanupContacts() {
        if (contactListener != null) {
            databaseReference.removeEventListener(contactListener);
            contactListener = null;
        }
        if (contactChildListener != null) {
            databaseReference.removeEventListener(contactChildListener);
            contactChildListener = null;
        }
    }

    /**
     * Fetch user details from Firebase and create contact if not exists
     * @param userId The user ID to fetch
     * @param callback Callback to handle the result
     */
    public void fetchAndCreateContact(String userId, final ContactsLoadListener callback) {
        if (userId == null || userId.isEmpty()) {
            callback.onError("Invalid user ID");
            return;
        }

        // First check if contact already exists
        Contact existingContact = getContactById(userId);
        if (existingContact != null) {
            callback.onContactAdded(existingContact);
            return;
        }

        // Fetch user details from Firebase
        databaseReference.child("users").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            String email = dataSnapshot.child("email").getValue(String.class);
                            String name = dataSnapshot.child("name").getValue(String.class);
                            
                            if (email != null && !email.isEmpty()) {
                                // Create new contact
                                addContact(userId, name != null ? name : email, email);
                                Contact newContact = getContactById(userId);
                                if (newContact != null) {
                                    callback.onContactAdded(newContact);
                                } else {
                                    callback.onError("Failed to create contact");
                                }
                            } else {
                                callback.onError("User has no email address");
                            }
                        } else {
                            callback.onError("User not found in Firebase");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        callback.onError(databaseError.getMessage());
                    }
                });
    }


    public void addContact(String contactId, String userName, String email) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            if (loadListener != null) {
                loadListener.onError("User not authenticated");
            }
            return;
        }

        // Check if contact already exists
        for (Contact contact : contactsCache.values()) {
            if (contact.getContactId().equals(contactId) || contact.getEmailAddress().equalsIgnoreCase(email)) {
                log.d(TAG, "Contact already exists");
                if (loadListener != null) {
                    loadListener.onContactAdded(contact);
                }
                return;
            }
        }

        // Create a new contact with the current user as the owner
        Contact newContact = new Contact();
        newContact.setUserId(currentUserId);
        newContact.setContactId(contactId);
        newContact.setUserName(userName);
        newContact.setEmailAddress(email);
        
        // Generate a unique key for the contact entry
        String contactKey = databaseReference.child(CONTACTS_NODE).child(currentUserId).push().getKey();
        
        if (contactKey == null) {
            if (loadListener != null) {
                loadListener.onError("Failed to create contact");
            }
            return;
        }

        // Save the contact to Firebase
        databaseReference.child(CONTACTS_NODE).child(currentUserId).child(contactKey)
                .setValue(newContact)
                .addOnSuccessListener(aVoid -> {
                    // Update the local cache
                    newContact.setId(contactKey);
                    contactsCache.put(contactId, newContact);
                    log.d(TAG, "Contact added successfully: " + email);
                    
                    // Notify listeners
                    if (loadListener != null) {
                        loadListener.onContactAdded(newContact);
                    }
                })
                .addOnFailureListener(e -> {
                    log.e(TAG, "Error adding contact: " + e.getMessage(), e);
                    if (loadListener != null) {
                        loadListener.onError(e.getMessage());
                    }
                });
    }

    public Contact getContactByEmail(String email) {
        for (Contact contact : contactsCache.values()) {
            if (contact.getEmailAddress().equalsIgnoreCase(email)) {
                return contact;
            }
        }
        return null;
    }

    public Contact getContactById(String contactId) {
        return contactsCache.get(contactId);
    }

    public List<String> getContactEmails() {
        List<String> emails = new ArrayList<>();
        for (Contact contact : contactsCache.values()) {
            emails.add(contact.getEmailAddress());
        }
        return emails;
    }

    private String getCurrentUserId() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    public Map<String, Contact> getContactsCache() {
        return contactsCache;
    }
    
    public Task<Void> removeContact(String contactId) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            if (loadListener != null) {
                loadListener.onError("User not authenticated");
            }
            return Tasks.forException(new IllegalStateException("User not authenticated"));
        }
        
        // Find the contact in the cache to get its key
        for (Map.Entry<String, Contact> entry : contactsCache.entrySet()) {
            if (entry.getValue().getContactId().equals(contactId)) {
                // Remove from Firebase and return the task
                return databaseReference.child(CONTACTS_NODE).child(currentUserId).child(entry.getKey())
                        .removeValue()
                        .addOnSuccessListener(aVoid -> {
                            // The onChildRemoved listener will handle updating the cache and notifying listeners
                            log.d(TAG, "Contact removed successfully: " + contactId);
                        })
                        .addOnFailureListener(e -> {
                            log.e(TAG, "Error removing contact: " + e.getMessage(), e);
                            if (loadListener != null) {
                                loadListener.onError("Failed to remove contact: " + e.getMessage());
                            }
                        });
            }
        }
        return Tasks.forException(new IllegalStateException("Contact not found"));
    }
    
    public List<Contact> getCachedContacts() {
        return new ArrayList<>(contactsCache.values());
    }
    
    /**
     * Cleans up all listeners and clears the cache.
     * Call this when the app is going to background or being destroyed.
     */
    public void cleanup() {
        String currentUserId = getCurrentUserId();
        if (currentUserId != null) {
            // Remove all listeners
            if (contactListener != null) {
                databaseReference.child(CONTACTS_NODE).child(currentUserId)
                        .removeEventListener(contactListener);
                contactListener = null;
            }
            if (contactChildListener != null) {
                databaseReference.child(CONTACTS_NODE).child(currentUserId)
                        .removeEventListener(contactChildListener);
                contactChildListener = null;
            }
        }
        
        // Clear the cache
        contactsCache.clear();
        
        // Clear the listener
        loadListener = null;
        previousListener = null;
        
        log.d(TAG, "ContactsManager cleaned up");
    }
}
