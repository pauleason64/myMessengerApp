USER STORY FOR SIMPLEMESSENGER APP
-----------------------------------
REQUIREMENT: ENHANCEMENTS TO NOTES FUNCTIONALITY.

DESIGN/IMPLEMENTATION CONSIDERATION:
For database activities we should utilise the existing "saveToFirebase" method. 

TESTING CONSIDERATION.
Firebase rules will need to be updated before testing

USER NEEDS
----------
NEED 1: -Ability for user to delete one or more notes from the Notes view 

Design consideration:
a) Provide a new "Trash" icon in the MessageList toolbar which will initially be disabled

Workflow:
a) User can long press on the note in the MessageList
b) The new trash icon is then enabled if it is not already
c) A new check-mark in the bottom right of the message item will be displayed
d) The check-mark will toggle on and off each time the user long presses the message
e) In the event the user opens another activity (for example switching to messages or short clicking a note) the list status will reset with the trash icon disabled again and any notes with a check-mark , the check-mark is removed.

NEED 2: - to support the deletion of 1 or more notes from the firebasedb from the MessageList view

Workflow:
a) User clicks on the new trash icon to trigger the delete note(s) workflow
b) app should display a dialog box requesting user to confirm deletion of {count} notes with OK and cancel options. If they cancel then the workflow is cancelled
c) app should create a list/map of all the noteIds (messageId?) with a check-mark and then create the required updates to send to firebase.
d) In the callback response from firebase then the notes view should be updated to remove the deleted notes. In the event the user deletes all their notes, the default empty messages list message should be displayed.
NEED 2:
a) Support forwarding of an existing note (being displayed in the MessageDetailActivity) 
b) Disable the FAB relating to the "reply" for Notes.

NEED 3: - to support the deletion of a single note from the firebasedb from the MessageDetail view

Workflow:
a) User clicks on the existing trash icon in the toolbar to trigger the delete note workflow
b) app should display a dialog box requesting user to confirm deletion of the note with OK and cancel options. If they cancel then the workflow is cancelled
c) app should create the required update to send to firebase.
d) In the callback response from firebase then the MessageDetail view should close and return back to the notes view 
In the event the user deletes all their notes, the default empty messages list message should be displayed.

NEED 4: - enforce the addition of a note title
a) When user composes a new note enforce the creation of a title (remove the (Optional) text from the textbox hint)
b) When the user attempts to save the note ensure that user has provided a title. if not display a red exclamation mark in the far right of the title field with popup hint that "This field is required". This validation rule already exists in the compose message view for messages with the Recipient email text control

