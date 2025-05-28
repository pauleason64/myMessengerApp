package com.example.simplemessenger.data;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.example.simplemessenger.data.model.Contact;
import com.example.simplemessenger.util.LogWrapper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(MockitoJUnitRunner.class)
public class ContactsManagerTest {

    @Mock
    private FirebaseDatabase mockDatabase;
    
    @Mock
    private DatabaseReference mockContactsRef;
    
    @Mock
    private DatabaseReference mockUserContactsRef;
    
    @Mock
    private DatabaseReference mockNewContactRef;
    
    @Mock
    private FirebaseAuth mockAuth;
    
    @Mock
    private FirebaseAuth.AuthStateListener mockAuthListener;
    
    @Mock
    private LogWrapper mockLogger;
    
    @Captor
    private ArgumentCaptor<ValueEventListener> valueEventListenerCaptor;
    
    @Captor
    private ArgumentCaptor<DatabaseReference.CompletionListener> completionListenerCaptor;
    
    private ContactsManager contactsManager;
    private final String TEST_USER_ID = "test_user_123";
    private final String TEST_CONTACT_ID = "test_contact_456";
    private final String TEST_EMAIL = "test@example.com";
    private final String TEST_NAME = "Test User";
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Set up mock auth
        when(mockAuth.getCurrentUser()).thenReturn(mock(com.google.firebase.auth.FirebaseUser.class));
        when(mockAuth.getCurrentUser().getUid()).thenReturn(TEST_USER_ID);
        
        // Initialize ContactsManager with mocks
        contactsManager = new ContactsManager(mockContactsRef, mockAuth, mockLogger);
    }
    
    @Test
    public void testAddContact_Success() throws InterruptedException {
        // Arrange
        CountDownLatch latch = new CountDownLatch(1);
        
        // Set up the database reference chain for the test
        DatabaseReference mockContactsNode = mock(DatabaseReference.class);
        DatabaseReference mockUserContactsNode = mock(DatabaseReference.class);
        DatabaseReference mockNewContactRef = mock(DatabaseReference.class);
        
        // Mock the database reference chain
        when(mockContactsRef.child("user-contacts")).thenReturn(mockContactsNode);
        when(mockContactsNode.child(TEST_USER_ID)).thenReturn(mockUserContactsNode);
        when(mockUserContactsNode.push()).thenReturn(mockNewContactRef);
        when(mockNewContactRef.getKey()).thenReturn("new_contact_key");
        
        // Mock the child reference for the new contact
        DatabaseReference mockNewContactChildRef = mock(DatabaseReference.class);
        when(mockUserContactsNode.child("new_contact_key")).thenReturn(mockNewContactChildRef);
        
        // Create a mock task for Firebase operations
        com.google.android.gms.tasks.Task<Void> mockTask = mock(com.google.android.gms.tasks.Task.class);
        
        // Mock the setValue operation
        when(mockNewContactChildRef.setValue(any(Contact.class))).thenReturn(mockTask);
        
        // Mock the success listener to call onSuccess immediately
        doAnswer(invocation -> {
            com.google.android.gms.tasks.OnSuccessListener<Void> listener = 
                invocation.getArgument(0);
            // Call onSuccess on a background thread to better simulate real behavior
            new Thread(() -> listener.onSuccess(null)).start();
            return mockTask;
        }).when(mockTask).addOnSuccessListener(any(com.google.android.gms.tasks.OnSuccessListener.class));
        
        // Mock the failure listener
        when(mockTask.addOnFailureListener(any(com.google.android.gms.tasks.OnFailureListener.class)))
            .thenReturn(mockTask);
        
        // Create a test listener to verify the results
        ContactsManager.ContactsLoadListener testListener = new ContactsManager.ContactsLoadListener() {
            @Override
            public void onContactsLoaded(List<Contact> contacts) {
                // Not used in this test
            }
            
            @Override
            public void onContactAdded(Contact contact) {
                try {
                    // Verify the contact was added successfully
                    assertNotNull("Contact should not be null", contact);
                    assertEquals("Email should match", TEST_EMAIL, contact.getEmailAddress());
                    assertEquals("Name should match", TEST_NAME, contact.getUserName());
                    assertEquals("User ID should match", TEST_USER_ID, contact.getUserId());
                    assertEquals("Contact ID should match", TEST_CONTACT_ID, contact.getContactId());
                } catch (AssertionError e) {
                    fail("Assertion failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            }
            
            @Override
            public void onError(String error) {
                fail("Failed to add contact: " + error);
                latch.countDown();
            }
        };
        
        // Act - Set the listener and add the contact
        contactsManager.setLoadListener(testListener);
        contactsManager.addContact(TEST_CONTACT_ID, TEST_NAME, TEST_EMAIL);
        
        // Assert - Wait for the async operation to complete
        assertTrue("Test timed out - contact was not added successfully", 
                  latch.await(2, TimeUnit.SECONDS));
        
        // Verify the contact was saved to the database
        verify(mockNewContactChildRef).setValue(any(Contact.class));
        
        // Verify logging - we can't verify the exact message due to dynamic content
        verify(mockLogger).d(anyString(), contains("Contact added successfully"));
    }
    
    @Test
    public void testLoadContacts() throws InterruptedException {
        // Arrange
        CountDownLatch latch = new CountDownLatch(1);
        
        // Set up the test data
        Contact testContact = new Contact();
        testContact.setUserId(TEST_USER_ID);
        testContact.setContactId(TEST_CONTACT_ID);
        testContact.setUserName(TEST_NAME);
        testContact.setEmailAddress(TEST_EMAIL);
        
        // Set up the database reference chain
        DatabaseReference mockContactsNode = mock(DatabaseReference.class);
        DatabaseReference mockUserContactsNode = mock(DatabaseReference.class);
        
        // Mock the database reference chain
        when(mockContactsRef.child("user-contacts")).thenReturn(mockContactsNode);
        when(mockContactsNode.child(TEST_USER_ID)).thenReturn(mockUserContactsNode);
        
        // Set up the test callback
        ContactsManager.ContactsLoadListener testListener = new ContactsManager.ContactsLoadListener() {
            @Override
            public void onContactsLoaded(List<Contact> contacts) {
                try {
                    // Verify the contacts were loaded successfully
                    assertNotNull("Contacts list should not be null", contacts);
                    assertFalse("Contacts list should not be empty", contacts.isEmpty());
                    
                    Contact loadedContact = contacts.get(0);
                    assertNotNull("Loaded contact should not be null", loadedContact);
                    assertEquals("Email should match", TEST_EMAIL, loadedContact.getEmailAddress());
                    assertEquals("Name should match", TEST_NAME, loadedContact.getUserName());
                    assertEquals("User ID should match", TEST_USER_ID, loadedContact.getUserId());
                    assertEquals("Contact ID should match", TEST_CONTACT_ID, loadedContact.getContactId());
                } catch (AssertionError e) {
                    fail("Assertion failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            }
            
            @Override
            public void onContactAdded(Contact contact) {}
            
            @Override
            public void onError(String error) {
                fail("Failed to load contacts: " + error);
                latch.countDown();
            }
        };
        
        // Act
        contactsManager.setLoadListener(testListener);
        contactsManager.loadContacts();
        
        // Simulate Firebase response with test data
        verify(mockUserContactsNode).addListenerForSingleValueEvent(valueEventListenerCaptor.capture());
        
        // Create a mock DataSnapshot with our test contact
        DataSnapshot mockContactSnapshot = mock(DataSnapshot.class);
        when(mockContactSnapshot.getValue(Contact.class)).thenReturn(testContact);
        when(mockContactSnapshot.getKey()).thenReturn(TEST_CONTACT_ID);
        
        // Create a mock Iterable for getChildren()
        Iterable<DataSnapshot> children = List.of(mockContactSnapshot);
        DataSnapshot mockSnapshot = mock(DataSnapshot.class);
        when(mockSnapshot.getChildren()).thenReturn(children);
        
        // Trigger the onDataChange callback with our mock snapshot
        valueEventListenerCaptor.getValue().onDataChange(mockSnapshot);
        
        // Wait for the async operation to complete
        assertTrue("Test timed out", latch.await(5, TimeUnit.SECONDS));
        
        // Verify the contact was added to the cache
        Contact cachedContact = contactsManager.getContactByEmail(TEST_EMAIL);
        assertNotNull("Contact should be in cache", cachedContact);
        assertEquals("Cached email should match", TEST_EMAIL, cachedContact.getEmailAddress());
    }
    
    @Test
    public void testGetContactByEmail() {
        // Arrange
        Contact testContact = new Contact();
        testContact.setUserId(TEST_USER_ID);
        testContact.setContactId(TEST_CONTACT_ID);
        testContact.setUserName(TEST_NAME);
        testContact.setEmailAddress(TEST_EMAIL);
        
        // Add the contact to the cache directly
        contactsManager.getContactsCache().put(TEST_EMAIL.toLowerCase(), testContact);
        
        // Act
        Contact foundContact = contactsManager.getContactByEmail(TEST_EMAIL);
        
        // Assert
        assertNotNull("Contact should be found", foundContact);
        assertEquals("Email should match", TEST_EMAIL, foundContact.getEmailAddress());
        assertEquals("Name should match", TEST_NAME, foundContact.getUserName());
        assertEquals("User ID should match", TEST_USER_ID, foundContact.getUserId());
        assertEquals("Contact ID should match", TEST_CONTACT_ID, foundContact.getContactId());
        
        // Test case insensitivity
        Contact foundContactUppercase = contactsManager.getContactByEmail(TEST_EMAIL.toUpperCase());
        assertNotNull("Contact should be found with uppercase email", foundContactUppercase);
        assertEquals("Email should match (case insensitive)", TEST_EMAIL, foundContactUppercase.getEmailAddress());
        
        // Test non-existent contact
        Contact nonExistentContact = contactsManager.getContactByEmail("nonexistent@example.com");
        assertNull("Non-existent contact should return null", nonExistentContact);
    }
    
    // Helper method to create a mock DataSnapshot
    private DataSnapshot createMockDataSnapshot(Contact contact) {
        DataSnapshot snapshot = mock(DataSnapshot.class);
        when(snapshot.getValue(Contact.class)).thenReturn(contact);
        when(snapshot.getKey()).thenReturn(contact.getContactId());
        return snapshot;
    }
}
