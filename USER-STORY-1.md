USER STORY FOR SIMPLEMESSENGER APP
-----------------------------------
REQUIREMENT: TO SUPPORT MESSAGE AND NOTE FORWARDING AS A NEW MESSAGE AND REPLYING TO MESSAGES:

DESIGN/IMPLEMENTATION CONSIDERATION:
The NEED 2/ NEED 3/ NEED 4  stories are very similar but with subtle differences. The code should be implemented with a single reusable Activity .

TESTING CONSIDERATION.
NEED 1 does not produce any meaningful testable code so should be tested alongside NEED 2

USER NEEDS
----------
NEED 1: Providing a message audit trail to support sending notes and messages and replying to messages
a) Create a new String field in the Message class - PreviousMessageId. (Usage of this field will be described in later User stories)
b) Update the Firebase rules so that new messages will optionally contain this value

NEED 2:
a) Support forwarding of an existing note (being displayed in the MessageDetailActivity) 
b) Disable the FAB relating to the "reply" for Notes.

Workflow for need 2:
-  Attach an action event to the user clicking on the "forward" FAB as follows: 
a)  Open the compose messages view. Title bar should read "forwarding note". The intent should be passed an "extra" with the messageId on which this action was requested.
b)  Insert 2 blank lines (carriage return) into the content text field.
Append the existing note content into the content field on the new message in this format:
Line 3 reads : "Saved note from xx on dd" where xx is a string consisting of the current user emailaddress (xx) and original note date (dd)"
Line 4 is blank:
Line 5 is the original note content.
b) the FAB for reply is disabled and has no functionality for Notes.
c) The cursor is placed at the beginning of the content field so that the user can insert appropriate text if needed. The existing content should move down the page as the user adds new text. 
d) When the user is ready to send the message use the existing functionality for sending messages. The new message should be updated with the  messageId which was saved when the intent was opened. This messageid should then be sent as part of the object map to firebase database.

NEED 3:
Support forwarding of an existing message from Inbox and Outbox(being displayed in the MessageDetailActivity) 

Workflow for need 3:
-  Attach an action event to the user clicking on the "forward" FAB as follows: 
a)  Open the compose messages view. Title bar should read "forwarding message". The intent should be passed an "extra" with the messageId on which this action was requested.
b)Insert 2 blank lines (carriage return) into the content text field.
Append the existing note content into the content field on the new message in this format:
Line 3 reads : "Forwarded message from xx on dd" where xx is a string consisting of the sending user emailaddress (xx) and original message date (dd)". For clarity :
- when the original message originates from the inbox then xx will be the original sender email 
- when the original message originates from the outbox then xx will be the current user email 
Line 4 is blank:
Line 5 is the original note content.
c) The cursor is placed at the beginning of the content field so that the user can insert appropriate text if needed. The existing content should move down the page as the user adds new text. 
d) When the user is ready to send the message use the existing functionality for sending messages. The new message should be updated with the  messageId which was saved when the intent was opened. This messageid should then be sent as part of the object map to firebase database.

NEED 4:
Support replying to an existing message from Inbox and Outbox (being displayed in the MessageDetailActivity) 

Workflow for need 4:
-  Replying to a message in the sent folder  is not required so the "reply" FAB should be disabled.
-  Attach an action event to the user clicking on the "forward" FAB as follows: 
a)  Open the compose messages view. Title bar should read "reply to message". The intent should be passed an "extra" with the messageId on which this action was requested.
b)Insert 2 blank lines (carriage return) into the content text field.
Append the existing note content into the content field on the new message in this format:
Line 3 reads : "Replying to message from xx on dd" where xx is a string consisting of the sending user emailaddress (xx) and original message date (dd)". For clarity xx will be the original sender email 
Line 4 is blank:
Line 5 is the original note content.
c) The cursor is placed at the beginning of the content field so that the user can insert appropriate text if needed. The existing content should move down the page as the user adds new text. 
d) The "To" text field shoud be pre-populated with the email of the address of the original sender.
e) When the user is ready to send the message use the existing functionality for sending messages. The new message should be updated with the  messageId which was saved when the intent was opened. This messageid should then be sent as part of the object map to firebase database.

