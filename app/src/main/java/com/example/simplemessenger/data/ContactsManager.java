package com.example.simplemessenger.data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.simplemessenger.util.FirebaseConfig;
import com.example.simplemessenger.util.FirebaseConfigManager;
import com.example.simplemessenger.util.FirebaseFactory;
import com.example.simplemessenger.util.LogWrapper;

import com.example.simplemessenger.data.model.Contact;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
        Log.d(TAG, "setLoadListener called with listener: " + (listener != null ? listener.getClass().getSimpleName() : "null"));
        
        // Store the previous listener before replacing it
        previousListener = loadListener;
        this.loadListener = listener;
        
        // If we have cached contacts and a new listener is set, notify the new listener
        if (listener != null && !contactsCache.isEmpty()) {
            Log.d(TAG, "Notifying new listener with " + contactsCache.size() + " cached contacts");
            listener.onContactsLoaded(new ArrayList<>(contactsCache.values()));
        } else if (listener != null) {
            Log.d(TAG, "No cached contacts to notify new listener");
        }
    }

    public void clearPreviousListener() {
        // Clear the previous listener to prevent memory leaks
        previousListener = null;
    }

    private ValueEventListener contactListener;
    private ChildEventListener contactChildListener;

    public void initializeContacts() {
        Log.d(TAG, "initializeContacts() called");
        
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            String errorMsg = "User not authenticated in initializeContacts()";
            Log.e(TAG, errorMsg);
            if (loadListener != null) {
                loadListener.onError(errorMsg);
            }
            return;
        }
        
        Log.d(TAG, "initializeContacts: Current user ID: " + currentUserId);
        
        // Clean up any existing listeners to prevent duplicates
        cleanupListeners();
        
        // If we have cached contacts, notify immediately
        if (!contactsCache.isEmpty()) {
            Log.d(TAG, "initializeContacts: Using " + contactsCache.size() + " cached contacts");
            if (loadListener != null) {
                loadListener.onContactsLoaded(new ArrayList<>(contactsCache.values()));
            }
            return;
        }

        // First load existing contacts
        Log.d(TAG, "Setting up contact listener for user: " + currentUserId);
        contactListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "onDataChange called with " + dataSnapshot.getChildrenCount() + " contacts");
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


    public void addContact(String email) {
        addContact(null, email);
    }
    
    /**
     * Updates a contact's display name
     * @param contactId The ID of the contact to update
     * @param newName The new display name
     * @param isCustomName Whether this is a custom name set by the user
     */
    public void updateContactName(String contactId, String newName, boolean isCustomName) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null || contactId == null || newName == null || newName.trim().isEmpty()) {
            return;
        }
        
        DatabaseReference contactRef = databaseReference
                .child(CONTACTS_NODE)
                .child(currentUserId)
                .child(contactId);
                
        Map<String, Object> updates = new HashMap<>();
        updates.put("displayName", newName.trim());
        updates.put("customName", isCustomName);
        
        contactRef.updateChildren(updates)
            .addOnSuccessListener(aVoid -> log.d(TAG, "Contact name updated successfully"))
            .addOnFailureListener(e -> log.e(TAG, "Failed to update contact name: " + e.getMessage()));
    }
    
    /**
     * Adds a new contact with the specified contact ID, display name, and email.
     * @param contactId The ID of the contact (can be null to auto-generate)
     * @param displayName The display name of the contact (can be null to use user's profile name)
     * @param email The email address of the contact
     */
    public void addContact(String contactId, String displayName, String email) {
        // If contactId is not provided, treat this as a regular add by email
        if (contactId == null || contactId.isEmpty()) {
            addContact(email);
            return;
        }
        
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            String error = "User not authenticated";
            log.e(TAG, error);
            if (loadListener != null) {
                loadListener.onError(error);
            }
            return;
        }
        
        // Validate email
        if (email == null || email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            String error = "Invalid email address: " + email;
            log.e(TAG, error);
            if (loadListener != null) {
                loadListener.onError(error);
            }
            return;
        }
        
        // Create a new contact with the provided details
        Contact newContact = new Contact();
        newContact.setContactId(contactId);
        newContact.setEmailAddress(email);
        newContact.setTimestamp(System.currentTimeMillis());
        
        // If displayName is provided, use it as the contact name
        // Otherwise, we'll use the user's profile name or email as fallback
        if (displayName != null && !displayName.trim().isEmpty()) {
            newContact.setDisplayName(displayName);
            newContact.setCustomName(true); // Mark that this is a custom name
        } else {
            // Default to email for now, will be updated after user lookup
            newContact.setDisplayName(email);
            newContact.setCustomName(false);
        }
        
        // First, try to get the user's profile information
        databaseReference.child("users").child(contactId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                    if (userSnapshot.exists()) {
                        // Get the user's display name from their profile
                        String userDisplayName = userSnapshot.child("displayName").getValue(String.class);
                        
                        // Only update the display name if it's not a custom name
                        if (!newContact.isCustomName() && userDisplayName != null && !userDisplayName.isEmpty()) {
                            newContact.setDisplayName(userDisplayName);
                        }
                    }
                    
                    // Save the contact with the updated information
                    saveContactToDatabase(newContact);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    log.e(TAG, "Error looking up user profile: " + databaseError.getMessage());
                    // Save with the information we have
                    saveContactToDatabase(newContact);
                }
            });
    }

/**
     * Adds a new contact with the specified contact ID and email.
     * @param contactId The ID of the contact (can be null to auto-generate)
     * @param email The email address of the contact (also used as display name if not provided)
     */
    public void addContact(String contactId, String email) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            String error = "User not authenticated";
            log.e(TAG, error);
            if (loadListener != null) {
                loadListener.onError(error);
            }
            return;
        }

        // Validate email
        if (email == null || email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            String error = "Invalid email address";
            log.e(TAG, error);
            if (loadListener != null) {
                loadListener.onError(error);
            }
            return;
        }

        // Generate a contact ID if not provided (for new contacts)
        final String finalContactId = (contactId != null && !contactId.isEmpty()) ? contactId : 
            FirebaseDatabase.getInstance().getReference(CONTACTS_NODE).child(currentUserId).push().getKey();

        // Check if contact already exists in cache by email
        for (Contact contact : contactsCache.values()) {
            if (email.equalsIgnoreCase(contact.getEmailAddress())) {
                log.d(TAG, "Contact already exists in cache: " + email);
                if (loadListener != null) {
                    loadListener.onContactAdded(contact);
                }
                return;
            }
        }

        // Create a new contact with minimal information
        final Contact newContact = new Contact();
        newContact.setUserId(currentUserId);
        newContact.setContactId(finalContactId);
        newContact.setEmailAddress(email);
        newContact.setUserName(email.split("@")[0]); // Default to first part of email
        newContact.setTimestamp(System.currentTimeMillis());

        // Try to look up user details from /users node
        databaseReference.child("users").orderByChild("email").equalTo(email.toLowerCase())
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // User found, update contact with their details
                        for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                            String displayName = userSnapshot.child("displayName").getValue(String.class);
                            if (displayName != null && !displayName.isEmpty()) {
                                newContact.setDisplayName(displayName);
                                newContact.setUserName(displayName);
                            }
                            // Update contactId to match the actual user's UID if this was a new contact
                            if (contactId == null || contactId.isEmpty()) {
                                newContact.setContactId(userSnapshot.getKey());
                            }
                            break; // Just use the first matching user
                        }
                    }
                    saveContactToDatabase(newContact);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // If user lookup fails, just save with the minimal info we have
                    log.e(TAG, "Error looking up user: " + databaseError.getMessage());
                    saveContactToDatabase(newContact);
                }
            });
    }

    private void saveContactToDatabase(Contact contact) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            log.e(TAG, "Cannot save contact - user not authenticated",null);
            return;
        }

        databaseReference.child(CONTACTS_NODE).child(currentUserId).child(contact.getContactId())
            .setValue(contact)
            .addOnSuccessListener(aVoid -> {
                if (!contactsCache.containsKey(contact.getContactId())) {
                    contact.setId(contact.getContactId());
                    contactsCache.put(contact.getContactId(), contact);
                    log.d(TAG, "Contact added/updated: " + contact.getEmailAddress());
                    
                    if (loadListener != null) {
                        loadListener.onContactAdded(contact);
                    }
                }
            })
            .addOnFailureListener(e -> {
                String errorMsg = "Failed to save contact: " + e.getMessage();
                log.e(TAG, errorMsg, e);
                if (loadListener != null) {
                    loadListener.onError(errorMsg);
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
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            Log.d(TAG, "Current user ID: " + user.getUid());
            return user.getUid();
        } else {
            Log.e(TAG, "No authenticated user found in getCurrentUserId()");
            return null;
        }
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
     * Cleans up all Firebase listeners without clearing the cache or load listener.
     * This is useful for reinitializing the listeners.
     */
    private void cleanupListeners() {
        Log.d(TAG, "Cleaning up Firebase listeners");
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            Log.w(TAG, "Cannot clean up listeners: No current user ID");
            return;
        }
        
        DatabaseReference contactsRef = databaseReference.child(CONTACTS_NODE).child(currentUserId);
        
        if (contactListener != null) {
            contactsRef.removeEventListener(contactListener);
            contactListener = null;
        }
        
        if (contactChildListener != null) {
            contactsRef.removeEventListener(contactChildListener);
            contactChildListener = null;
        }
        
        Log.d(TAG, "Firebase listeners cleaned up");
    }
    
    /**
     * Cleans up all listeners and clears the cache.
     * Call this when the app is going to background or being destroyed.
     */
    public void cleanup() {
        Log.d(TAG, "Starting cleanup of ContactsManager");
        cleanupListeners();
        
        // Clear the cache
        contactsCache.clear();
        
        // Clear the listeners
        loadListener = null;
        previousListener = null;
        
        Log.d(TAG, "ContactsManager cleanup completed");
    }
}
