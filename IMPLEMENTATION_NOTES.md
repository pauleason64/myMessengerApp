# SimpleMessenger Implementation Notes

## Table of Contents
- [Note and Message Composition](#note-and-message-composition)
- [Navigation and UI Updates](#navigation-and-ui-updates)
- [Key Components](#key-components)
- [Testing Notes](#testing-notes)

## Note and Message Composition

### Features
- Single `ComposeMessageActivity` handles both notes and messages
- Toggle between modes with UI updates
- Different behavior and validation for each mode
- Dynamic FAB icon and behavior based on mode

### Implementation Details
- `isNoteMode` flag controls the behavior
- Different validation rules for notes vs messages
- Context-aware UI updates
- Proper focus management between fields

### Intent Extras
```java
public static final String EXTRA_IS_NOTE = "is_note";
public static final String EXTRA_NOTE_MODE = "note_mode";
public static final String EXTRA_COMPOSE_NEW = "compose_new";
```

## Navigation and UI Updates

### MainActivity FAB Handling
- Shows compose icon for Inbox/Outbox
- Shows note icon for Notes tab
- Hidden on Contacts tab
- Launches ComposeMessageActivity with appropriate mode

### UI Updates
- Dynamic titles and hints
- Show/hide recipient field based on mode
- Update FAB icon and accessibility content
- Proper focus management

## Key Components

### ComposeMessageActivity
- Handles both note and message composition
- Dynamic UI updates based on mode
- Proper validation and error handling
- Database operations for saving notes/messages

### MainActivity
- Bottom navigation with ViewPager2
- FAB management
- Fragment management for different sections
- Navigation handling

## Testing Notes

### Note Creation
1. Navigate to Notes tab
2. Tap FAB to create new note
3. Verify:
   - Title hint shows "Note Title"
   - No recipient field visible
   - Save button shows disk icon
   - Proper keyboard focus

### Message Sending
1. Navigate to Inbox/Outbox
2. Tap FAB to compose message
3. Verify:
   - Recipient field is visible and required
   - Subject hint shows "Subject"
   - Send button shows send icon
   - Proper validation for required fields

### Mode Toggling
1. In compose screen, toggle between note/message modes
2. Verify UI updates correctly
3. Check that form state is preserved

### Accessibility
- Verify content descriptions for all interactive elements
- Test with TalkBack enabled
- Check focus order and announcements

## Known Issues
- None currently identified

## Future Improvements
- Add rich text formatting for notes
- Support attachments in messages
- Add search functionality for notes/messages
