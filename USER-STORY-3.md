USER STORY FOR SIMPLEMESSENGER APP
-----------------------------------
REQUIREMENT: TO SUPPORT ADDIING A "CATAGORY" TYPE TO A NOTE.

TESTING CONSIDERATION.
Update the firebase rules to add a new Optional field "Category" (type string) in the /messages node
Update the firebase rules to add the new /user-category/{uid}/categoryList record in json format.
Update the firebase rules to add a new index "category" in the /user-messages/{uid}/notes folder so searches by category can be performed in a later requirement.

USER NEEDS
----------
NEED 1: Provide a default list of note category to be created for each user. 

1. create a new file in the resources folder - defaultCatagories.json and populate this with the default values:
"Birthday", "Anniversary","Event","Shopping List","Reminder","Secrets".

NEED 2: -New Class Category in the application and set default values 

Design consideration:
This will be used in a later requirement

Workflow:
1.Create a new user.
2. Once they login for the first time:
a) Create the user record in the database as today
b) Create a new file under /user-category/{uid}/userCatagories.json saving the default values that are loaded at startup.
3) Validate the catagories file is created.
4) The subsequent times a user logs in, load the userCatagories.json file into a new "catagoryCache" which will be referenced in future needs

NEED 3: - to support the displaying of Catagories in the notesView

Workflow:
1) Update the composeMessageActivity to support the following:
a) add a new dropdown list under the Subject/Title item in the layout file. this will only be visible when the message type isNote
b) when a user creates a new note, the form will be populated with the values from the catagoryCache which was loaded at startup. The default value should be "Reminder"
c) When the user saves the note, the String value of the category selected will be saved with the message object to the db. 


User story 4 will contain further requirements to support searching and filtering of notes in the messageList view and the ability to add new catagories.
