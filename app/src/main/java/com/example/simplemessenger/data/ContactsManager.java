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
    /**
     * Callback interface for database operations
     */
    public interface DatabaseCallback {
        void onSuccess(Object result);
        void onError(String error);
    }

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
     * Returns the in-memory cache of contacts.
     * @return A map of contact IDs to Contact objects.
     */
    public Map<String, Contact> getContactsCache() {
        return contactsCache;
    }

    /**
     * Fetch user details from Firebase and create contact if not exists
     * @param userIdOrEmail The user ID or email to fetch
     * @param callback Callback to handle the result
     */
    public void fetchAndCreateContact(String userIdOrEmail, final ContactsLoadListener callback) {
        Log.d(TAG, "[fetchAndCreateContact] Starting lookup for: " + userIdOrEmail);
        
        if (userIdOrEmail == null || userIdOrEmail.isEmpty()) {
            Log.e(TAG, "[fetchAndCreateContact] Error: userIdOrEmail is null or empty");
            callback.onError("Invalid user ID or email");
            return;
        }
        
        Log.d(TAG, "[fetchAndCreateContact] 1. Input: " + userIdOrEmail);
        
        // First check if contact already exists in cache by ID
        Contact existingContact = getContactById(userIdOrEmail);
        if (existingContact != null) {
            Log.d(TAG, "[fetchAndCreateContact] 2. Found existing contact in cache by ID: " + existingContact.getContactId());
            callback.onContactAdded(existingContact);
            return;
        } else {
            Log.d(TAG, "[fetchAndCreateContact] 2. No contact found in cache by ID");
        }
        
        // If the input looks like a UID (alphanumeric and at least 16 chars)
        boolean isUidPattern = userIdOrEmail.matches("^[a-zA-Z0-9]{16,}$");
        Log.d(TAG, "[fetchAndCreateContact] 3. Input " + (isUidPattern ? "matches" : "does not match") + " UID pattern");
        
        if (isUidPattern) {
            Log.d(TAG, "[fetchAndCreateContact] 4. Checking users node for UID: " + userIdOrEmail);
            databaseReference.child("users").child(userIdOrEmail)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                        Log.d(TAG, "[fetchAndCreateContact] 5. UID lookup result - exists: " + userSnapshot.exists());
                        
                        if (userSnapshot.exists()) {
                            String uid = userSnapshot.getKey();
                            String email = userSnapshot.child("email").getValue(String.class);
                            String name = userSnapshot.child("name").getValue(String.class);
                            
                            Log.d(TAG, String.format("[fetchAndCreateContact] 6. Found user by UID - UID: %s, Email: %s, Name: %s", 
                                uid, email, name));
                            
                            if (email != null && !email.isEmpty()) {
                                Log.d(TAG, "[fetchAndCreateContact] 7. Adding contact from UID lookup");
                                addContact(uid, name != null ? name : email, email, new DatabaseCallback() {
                                    @Override
                                    public void onSuccess(Object result) {
                                        Log.d(TAG, "[fetchAndCreateContact] 8. Successfully created contact from UID");
                                        Contact newContact = getContactById(uid);
                                        if (newContact != null) {
                                            callback.onContactAdded(newContact);
                                        } else {
                                            Log.e(TAG, "[fetchAndCreateContact] 8. Contact created but not found in cache");
                                            callback.onError("Contact created but not found in cache");
                                        }
                                    }

                                    @Override
                                    public void onError(String error) {
                                        Log.e(TAG, "[fetchAndCreateContact] 8. Failed to create contact from UID: " + error);
                                        callback.onError("Failed to create contact: " + error);
                                    }
                                });
                                return;
                            } else {
                                Log.e(TAG, "[fetchAndCreateContact] 7. Email is null or empty in user data");
                            }
                        } else {
                            Log.d(TAG, "[fetchAndCreateContact] 6. No user found with UID: " + userIdOrEmail);
                        }
                        
                        // If we get here, either user doesn't exist or we couldn't create contact
                        Log.d(TAG, "[fetchAndCreateContact] 9. Falling back to email check after UID lookup");
                        checkEmailInAuthAndDatabase(userIdOrEmail, callback);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "[fetchAndCreateContact] 5. Database error in UID lookup: " + databaseError.getMessage());
                        Log.d(TAG, "[fetchAndCreateContact] 6. Falling back to email check after UID lookup error");
                        checkEmailInAuthAndDatabase(userIdOrEmail, callback);
                    }
                });
        } else {
            Log.d(TAG, "[fetchAndCreateContact] 4. Treating input as email");
            // If input doesn't look like a UID, treat it as an email
            checkEmailInAuthAndDatabase(userIdOrEmail, callback);
        }
    }
    
    private void checkEmailInAuthAndDatabase(String email, final ContactsLoadListener callback) {
        Log.d(TAG, "1. Starting lookup for email: " + email);
        
        // First check if contact exists by email in cache
        Contact existingContact = getContactByEmail(email);
        if (existingContact != null) {
            Log.d(TAG, "2. Found existing contact in cache by email");
            callback.onContactAdded(existingContact);
            return;
        }

        Log.d(TAG, "3. Checking database for user with email: " + email.toLowerCase());
        databaseReference.child("users")
            .orderByChild("email")
            .equalTo(email.toLowerCase())
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Log.d(TAG, "4. Database query completed. Data exists: " + dataSnapshot.exists() + 
                        ", has children: " + dataSnapshot.hasChildren());
                        
                    if (dataSnapshot.exists() && dataSnapshot.hasChildren()) {
                        try {
                            // Get the first user with this email (should be only one)
                            DataSnapshot userSnapshot = dataSnapshot.getChildren().iterator().next();
                            String uid = userSnapshot.getKey();
                            String userEmail = userSnapshot.child("email").getValue(String.class);
                            String name = userSnapshot.child("name").getValue(String.class);
                            
                            Log.d(TAG, "5. Found user in database - UID: " + uid + 
                                ", Email: " + userEmail + ", Name: " + name);
                            
                            if (uid == null || userEmail == null) {
                                Log.e(TAG, "6. Invalid user data - UID or email is null");
                                callback.onError("Invalid user data in database");
                                return;
                            }
                            
                            if (name == null) {
                                // If no name in database, use email prefix as name
                                name = userEmail.contains("@") ? 
                                    userEmail.substring(0, userEmail.indexOf('@')) : 
                                    userEmail;
                                Log.d(TAG, "7. Using email prefix as name: " + name);
                            }
                            
                            // Create a temporary contact object with the user's details
                            Contact tempContact = new Contact();
                            tempContact.setContactId(uid);
                            tempContact.setEmailAddress(userEmail);
                            tempContact.setDisplayName(name);
                            tempContact.setCustomName(false);
                            
                            // Return the contact immediately
                            Log.d(TAG, "8. Returning temporary contact for immediate use");
                            callback.onContactAdded(tempContact);
                            
                            // Save the contact in the background
                            Log.d(TAG, "9. Saving contact in background");
                            addContact(uid, name, userEmail, new DatabaseCallback() {
                                @Override
                                public void onSuccess(Object result) {
                                    Log.d(TAG, "10. Successfully saved contact in background: " + uid);
                                }
                                
                                @Override
                                public void onError(String error) {
                                    Log.e(TAG, "11. Error saving contact in background: " + error);
                                }
                            });
                            
                        } catch (Exception e) {
                            Log.e(TAG, "12. Error processing user data: " + e.getMessage(), e);
                            callback.onError("Error processing user data");
                        }
                    } else {
                        Log.d(TAG, "13. No user found in database with email: " + email);
                        callback.onError("No user found with this email");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "14. Database error: " + databaseError.getMessage(), databaseError.toException());
                    callback.onError("Error looking up user in database: " + databaseError.getMessage());
                }
            });
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
    /**
     * Adds a new contact with the specified contact ID, display name, and email.
     * @param contactId The ID of the contact (can be null to use email as ID)
     * @param displayName The display name of the contact (can be null to use email as name)
     * @param email The email address of the contact
     */
    public void addContact(String contactId, String displayName, String email, DatabaseCallback callback) {
        // If email is not provided but contactId is, use contactId as email
        if ((email == null || email.isEmpty()) && contactId != null) {
            email = contactId;
        }
        
        // If we still don't have an email, we can't proceed
        if (email == null || email.isEmpty()) {
            String error = "Email is required to add a contact";
            log.e(TAG, error);
            if (callback != null) {
                callback.onError(error);
            }
            return;
        }
        
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            String error = "User not authenticated";
            log.e(TAG, error);
            if (callback != null) {
                callback.onError(error);
            }
            return;
        }
        
        // If contactId is not provided, use the email as a fallback for the contact ID
        // But we should have the actual user ID from the lookup
        if (contactId == null || contactId.isEmpty()) {
            contactId = email; // Fallback, but this should be the actual user ID from the lookup
        }
        
        // Create a new contact with the provided details
        Contact newContact = new Contact();
        newContact.setUserId(contactId); // This should be the recipient's user ID, not the current user's ID
        newContact.setContactId(contactId); // Use the same ID for contactId
        newContact.setEmailAddress(email);
        newContact.setTimestamp(System.currentTimeMillis());
        
        // Set display name (use provided name, email prefix, or contactId as fallback)
        if (displayName != null && !displayName.trim().isEmpty()) {
            newContact.setDisplayName(displayName);
            newContact.setCustomName(true);
        } else if (email != null && email.contains("@")) {
            newContact.setDisplayName(email.substring(0, email.indexOf('@')));
            newContact.setCustomName(false);
        } else {
            newContact.setDisplayName(contactId);
            newContact.setCustomName(false);
        }
        
        // Save the contact
        saveContactToDatabase(newContact, new DatabaseCallback() {
            @Override
            public void onSuccess(Object result) {
                Log.d(TAG, "Contact saved successfully: " + (result != null ? result.toString() : "null"));
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error saving contact: " + error);
            }
        });
    }

    /**
     * Adds a new contact with the specified contact ID and email.
     * @param contactId The ID of the contact (can be null to use email as ID)
     * @param email The email address of the contact (also used as display name if not provided)
     */
    public void addContact(String contactId, String email, DatabaseCallback callback) {
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
            FirebaseFactory.getDatabase().getReference(CONTACTS_NODE).child(currentUserId).push().getKey();

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


        DatabaseCallback dcb=new DatabaseCallback() {
            @Override
            public void onSuccess(Object result) {
                Log.d(TAG, "Contact saved successfully: " + (result != null ? result.toString() : "null"));
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error saving contact: " + error);
            }
        };

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
                    saveContactToDatabase(newContact,dcb);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // If user lookup fails, just save with the minimal info we have
                    log.e(TAG, "Error looking up user: " + databaseError.getMessage());
                    saveContactToDatabase(newContact, dcb);
                }
            });
    }

    private void saveContactToDatabase(Contact contact, DatabaseCallback callback) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            String error = "Cannot save contact - user not authenticated";
            log.e(TAG, error, null);
            if (callback != null) {
                callback.onError(error);
            }
            return;
        }

        // Make sure we have a valid contact ID
        if (contact.getContactId() == null || contact.getContactId().isEmpty()) {
            String error = "Cannot save contact - invalid contact ID";
            log.e(TAG, error);
            if (callback != null) {
                callback.onError(error);
            }
            return;
        }

        // Make sure we have a valid user ID for the contact
        if (contact.getUserId() == null || contact.getUserId().isEmpty()) {
            contact.setUserId(contact.getContactId()); // Use contact ID as user ID if not set
        }

        log.d(TAG, "Saving contact - ID: " + contact.getContactId() + 
              ", UserID: " + contact.getUserId() + 
              ", Email: " + contact.getEmailAddress());

        // Save to the current user's contacts
        databaseReference.child(CONTACTS_NODE).child(currentUserId).child(contact.getContactId())
            .setValue(contact)
            .addOnSuccessListener(aVoid -> {
                // Update cache
                contact.setId(contact.getContactId());
                contactsCache.put(contact.getContactId(), contact);
                log.d(TAG, "Contact added/updated: " + contact.getEmailAddress() + " (ID: " + contact.getContactId() + ")");
                
                // Notify listeners
                if (loadListener != null) {
                    loadListener.onContactAdded(contact);
                }
                
                // Call the callback
                if (callback != null) {
                    callback.onSuccess(contact);
                }
            })
            .addOnFailureListener(e -> {
                String errorMsg = "Failed to save contact: " + e.getMessage();
                log.e(TAG, errorMsg, e);
                if (loadListener != null) {
                    loadListener.onError(errorMsg);
                }
                if (callback != null) {
                    callback.onError(errorMsg);
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
