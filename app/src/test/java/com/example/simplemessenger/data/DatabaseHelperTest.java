package com.example.simplemessenger.data;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.example.simplemessenger.data.model.Message;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.mockito.quality.Strictness;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.junit.Rule;

@RunWith(MockitoJUnitRunner.Silent.class)
public class DatabaseHelperTest {

    @Mock
    private FirebaseDatabase mockDatabase;
    
    @Mock
    private DatabaseReference mockRootRef;
    
    @Mock
    private DatabaseReference mockMessagesRef;
    
    @Mock
    private DatabaseReference mockUserMessagesRef;
    
    @Mock
    private DatabaseReference mockSentRef;
    
    @Mock
    private DatabaseReference mockReceivedRef;
    
    @Mock
    private DatabaseReference mockNewMessageRef;
    
    @Mock
    private FirebaseAuth mockAuth;
    
    @Captor
    private ArgumentCaptor<ValueEventListener> valueEventListenerCaptor;
    
    @Captor
    private ArgumentCaptor<DatabaseReference.CompletionListener> completionListenerCaptor;
    
    private DatabaseHelper databaseHelper;
    private final String TEST_USER_ID = "test_user_123";
    private final String TEST_RECIPIENT_ID = "test_recipient_456";
    private final String TEST_MESSAGE_ID = "test_message_789";
    private final String TEST_EMAIL = "test@example.com";
    private final String TEST_RECIPIENT_EMAIL = "recipient@example.com";
    
    // Constants from DatabaseHelper
    private static final String MESSAGES_NODE = "messages";
    private static final String USER_MESSAGES_NODE = "user-messages";
    private static final String USER_SENT_NODE = "sent";
    private static final String USER_RECEIVED_NODE = "received";
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Set up auth mock that will be used in tests
        com.google.firebase.auth.FirebaseUser mockUser = mock(com.google.firebase.auth.FirebaseUser.class);
        when(mockAuth.getCurrentUser()).thenReturn(mockUser);
        when(mockUser.getUid()).thenReturn(TEST_USER_ID);
        when(mockUser.getEmail()).thenReturn(TEST_EMAIL);
        
        // Initialize DatabaseHelper with mocks
        databaseHelper = new DatabaseHelper(mockRootRef, mockAuth);
        
        // Reset mocks to ensure clean state between tests
        reset(mockRootRef, mockMessagesRef, mockUserMessagesRef, mockSentRef, mockReceivedRef, mockNewMessageRef);
    }
    
    @Test
    public void testSendMessage_Success() throws InterruptedException {
        // Arrange
        CountDownLatch latch = new CountDownLatch(1);
        
        // Create a test message
        Message testMessage = new Message();
        testMessage.setSenderId(TEST_USER_ID);
        testMessage.setRecipientId(TEST_RECIPIENT_ID);
        testMessage.setSenderEmail(TEST_EMAIL);
        testMessage.setRecipientEmail(TEST_RECIPIENT_EMAIL);
        testMessage.setContent("Test message content");
        
        // Mock the updateChildren() call
        Task<Void> mockTask = mock(Task.class);
        when(mockTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            OnSuccessListener<Void> listener = invocation.getArgument(0);
            // Simulate task success
            listener.onSuccess(null);
            return mockTask;
        });
        when(mockTask.addOnFailureListener(any())).thenReturn(mockTask);
        
        // Mock the database reference for messages
        when(mockRootRef.child(MESSAGES_NODE)).thenReturn(mockMessagesRef);
        when(mockMessagesRef.push()).thenReturn(mockNewMessageRef);
        when(mockNewMessageRef.getKey()).thenReturn(TEST_MESSAGE_ID);
        
        // The recipient ID is derived from the email in DatabaseHelper using getUserIdFromEmail()
        String expectedRecipientId = TEST_RECIPIENT_EMAIL.replace("@", "_").replace(".", "_");
        
        // Mock the updateChildren call
        when(mockRootRef.updateChildren(any(Map.class))).thenReturn(mockTask);
        
        // Mock the user messages references
        when(mockRootRef.child(USER_MESSAGES_NODE)).thenReturn(mockUserMessagesRef);
        
        // Set up test callback
        DatabaseHelper.DatabaseCallback testCallback = new DatabaseHelper.DatabaseCallback() {
            @Override
            public void onSuccess(Object result) {
                if (result instanceof String) {
                    assertEquals("Message ID should match", TEST_MESSAGE_ID, result);
                } else if (result != null) {
                    fail("Unexpected result type: " + result.getClass().getName());
                }
                latch.countDown();
            }

            @Override
            public void onError(String error) {
                fail("Failed to send message: " + error);
                latch.countDown();
            }
        };
        
        // Act
        databaseHelper.sendMessage(testMessage, testCallback);
        
        // Wait for the async operation to complete
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        
        // Verify the message was saved using updateChildren with the correct paths
        ArgumentCaptor<Map<String, Object>> updatesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockRootRef).updateChildren(updatesCaptor.capture());
        
        // Get the captured updates map
        Map<String, Object> updates = updatesCaptor.getValue();
        assertNotNull("Updates map should not be null", updates);
        
        // Verify the expected paths are in the updates
        String messagesPath = "/" + MESSAGES_NODE + "/" + TEST_MESSAGE_ID;
        String sentPath = "/" + USER_MESSAGES_NODE + "/" + TEST_USER_ID + "/" + USER_SENT_NODE + "/" + TEST_MESSAGE_ID;
        String receivedPath = "/" + USER_MESSAGES_NODE + "/" + expectedRecipientId + "/" + USER_RECEIVED_NODE + "/" + TEST_MESSAGE_ID;
        
        // Verify all expected paths exist in the updates
        assertTrue("Should contain messages path: " + messagesPath, updates.containsKey(messagesPath));
        assertTrue("Should contain sent path: " + sentPath, updates.containsKey(sentPath));
        assertTrue("Should contain received path: " + receivedPath, updates.containsKey(receivedPath));
    }
    
    @Test
    public void testGetMessage() throws InterruptedException {
        // Arrange
        CountDownLatch latch = new CountDownLatch(1);
        
        // Set up the database reference for messages
        when(mockRootRef.child(MESSAGES_NODE)).thenReturn(mockMessagesRef);
        
        // Create a test message
        Message expectedMessage = new Message();
        expectedMessage.setId(TEST_MESSAGE_ID);
        expectedMessage.setSenderId(TEST_USER_ID);
        expectedMessage.setRecipientId(TEST_RECIPIENT_ID);
        expectedMessage.setSenderEmail(TEST_EMAIL);
        expectedMessage.setRecipientEmail(TEST_RECIPIENT_EMAIL);
        expectedMessage.setContent("Test message content");
        expectedMessage.setTimestamp(System.currentTimeMillis());
        
        // Set up mock for the message reference
        DatabaseReference mockMessageRef = mock(DatabaseReference.class);
        when(mockMessagesRef.child(TEST_MESSAGE_ID)).thenReturn(mockMessageRef);
        
        // Set up the mock for addListenerForSingleValueEvent
        doAnswer(invocation -> {
            ValueEventListener listener = invocation.getArgument(0);
            
            // Create a mock DataSnapshot with our test message
            DataSnapshot mockSnapshot = mock(DataSnapshot.class);
            when(mockSnapshot.exists()).thenReturn(true);
            when(mockSnapshot.getValue(Message.class)).thenReturn(expectedMessage);
            when(mockSnapshot.getKey()).thenReturn(TEST_MESSAGE_ID);
            
            // Simulate the Firebase callback
            listener.onDataChange(mockSnapshot);
            return null;
        }).when(mockMessageRef).addListenerForSingleValueEvent(any(ValueEventListener.class));
        
        // Set up test callback
        DatabaseHelper.DatabaseCallback testCallback = new DatabaseHelper.DatabaseCallback() {
            @Override
            public void onSuccess(Object result) {
                try {
                    assertTrue("Result should be a Message", result instanceof Message);
                    Message message = (Message) result;
                    assertNotNull("Message should not be null", message);
                    assertEquals("Message ID should match", TEST_MESSAGE_ID, message.getId());
                    assertEquals("Sender ID should match", TEST_USER_ID, message.getSenderId());
                    assertEquals("Recipient ID should match", TEST_RECIPIENT_ID, message.getRecipientId());
                    assertEquals("Content should match", "Test message content", message.getContent());
                    assertNotNull("Timestamp should be set", message.getTimestamp());
                } catch (AssertionError e) {
                    fail("Assertion failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            }
            
            @Override
            public void onError(String error) {
                fail("Failed to get message: " + error);
                latch.countDown();
            }
        };
        
        // Act
        databaseHelper.getMessage(TEST_MESSAGE_ID, testCallback);
        
        // Wait for the async operation to complete
        assertTrue("Test timed out", latch.await(5, TimeUnit.SECONDS));
        
        // Verify the correct database reference was used
        verify(mockRootRef).child(MESSAGES_NODE);
        verify(mockMessagesRef).child(eq(TEST_MESSAGE_ID));
        verify(mockMessageRef).addListenerForSingleValueEvent(any(ValueEventListener.class));
    }
}
