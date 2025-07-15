Feature: Query Builder 


Scenario: Accessing Query Group create page 
	Given the user has logged in
    And the user has navigated to the Query Group page
    Then there is a Query Group section
    And there is a field to add Query Group Name 
    And there is a field to add Query Group Description
    And there is a button to add a Rule 
    And there is a button to add a Ruleset  
    And there is a "Add Content Group" button 
    And there is a button to Save the whole query group 


Scenario: Adding a new rule 
	Given the user is on the Query Builder page
	And the user clicks the "Rule" button 

	Then a new rule is added 
	And there is a dropdown for Type
	And there is a field for Metric 
	And there is a field for Comparison Operator
	And there is a field for Value 
	And there is a dropdown for timeframe 
	And a new rule is added with these default values:
    | Passive data | Heart Rate | = | empty | empty |



Scenario: Selecting a Type for a query 
	Given the user clicked the "Rule" button
	Then the user can change the Type for that particular query
	And the user can choose from the following options: 

	| Passive data |
	| Questionnaire Slider |
	| Questionnaire Multichoice | 
	| Questionnaire Group |
	| Questionnaire Delusions |


Scenario: Metrics for Passive data
	Given the user has chosen "Passive Data"
	Then the following comparison operators can be chosen: "<",">","<=",">=","==","!="
	And the following metrics can be chosen: 
	| Heart Rate |
	| HRV |
	| Breathing Rate | 
	| Steps |
	| Stairs |
	| Sleep Duration |
	| Activities |
	| Exercise Duration |


Scenario: Metrics for Questionnaire Slider
 Given the user has chosen "Questionnaire Slider"
 Then the following comparison operators can be chosen: "<",">","<=",">=","==","!="
 And all the slider type questions from the CONNECT questionnaire set can be chosen 


Scenario: Metrics for Questionnaire Multichoice
 Given the user has chosen "Questionnaire Multichoice"
 Then the following comparison operators can be chosen: "IS"
 And  all the histogram questions from the CONNECT questionnaire set can be chosen 

Scenario: Metrics for Questionnaire Group
 Given the user has chosen "Questionnaire Group"
 Then the following comparison operators can be chosen: "<",">","<=",">=","==","!="
 And all the questionnaire groups from the CONNECT questionnaire set can be chosen 

Scenario: Metrics for Questionnaire Delusions
 Given the user has chosen "Questionnaire Delusions"
 Then the following comparison operators can be chosen: "<",">","<=",">=","==","!="
 And all the delusion questions from the CONNECT questionnaire set can be chosen 
 

Scenario: Adding a new Ruleset
	Given the user is on the Query Builder page
	And the user clicks the "Ruleset" button 
	Then an inner query is create 
	And there is a choice between AND and OR 
	And there is a message saying "A ruleset cannot be empty. Please add a rule or remove it all together"


Scenario: Adding a new content group
	Given the user has clicked the "Add Content Group" button 
	Then a form for adding a new content group appears 
	And there is a field for Content Group Name 
	And there is a field for Title 
	And there are four buttons: "Add Text", "Add Video", "Add Image", "Add module link"


Scenario: Adding a text section to the content group 
   Given the user is adding a new content group 
   And the user has clicked "Add Text" button 
   Then a heading field is displayed 
   And and a text editor is displayed 
   And there is a "Delete" button for that particular item 


Scenario: Adding a video item to the content group 
   Given the user is adding a new content group 
   And the user has clicked "Add Video" button 
   Then a modal is displayed 
   And there is a field for Youtube URL 
   And there is a "Add" button
   And there is a "Close" button 



 Scenario: Clicking Add button in the Add video modal
 	Given the user has clicked "Add Video" button
 	And the user has provided a youtube URL
 	And the user clicked "Add" button
 	Then a video item is added to the content group 



 Scenario: Clicking Close button in the Add video modal
 	Given the user has clicked "Add Video" button
 	And the user has provided a youtube URL
 	And the user clicked "Close" button
 	Then the modal is closed
 	And no video item is added to the content group 


 Scenario: Adding an image item to the content group 
   Given the user is adding a new content group 
   And the user has clicked "Add Image" button 
   Then a modal is displayed 
   And there is a "Choose file" button 
   And there is a "Add" button
   And there is a "Close" button 


 Scenario: Selecting an image in the Add Image modal 
 	Given the Add Image modal is opened
 	And the user clicked the "Choose file" button 
 	Then a file explorer is opened 
 	And the user can choose an image file to add 


  Scenario: Clicking Add button in the Add Image modal
 	Given the Add Image modal is opened
 	And the user selected an image
 	And the user clicked "Add" button 
	Then a video item is added to the content group 

  Scenario: Clicking Close button in the Add Image modal
 	Given the Add Image modal is opened
 	And the user selected an image
 	And the user clicked "Close" button 
	Then no image item is added to the content group 



 Scenario: Adding a module link to the content group 
   Given the user is adding a new content group 
   And the user has clicked "Add module link" button 
   Then a new dropdwon item is added to the content group 
   And the user can select from a predefined set of tools 


  Scenario: Clicking a delete button for any of the items in the content group
  	Given the user is adding a new content group
  	And the user clicks "Delete" button for any of the added items 
  	Then the item is deleted from the content group 


  Scenario: Saving a content group 
  	Given the user is adding a new content group
  	And the user added some items to it
  	And the user clicks "Save" button 
  	Then the content group is added to the query builder 
  	And the added content group has a title
  	And there is a "Delete" button for each added content group 
  	And there is an information how many items the content group has 


  Scenario: Activating a content group 
   Given a content group has been created 
   And the content group is not activated
   When the user activates the content group
   Then the content group can be assigned to any participant by the system


   Scenario: Deactivating a content group
	   Given a content group has been created 
	   And the content group is activated 
	   When the user deactives teh content group
	   Then the content group is deleted from participants' phones 


  Scenario: Deleting a content group 
  	Given the user added a content group 
  	And the user clicks "Delete" button for that content group 
  	Then the content group is deleted from the query builder 



  Scenario: Saving the whole query builder including the content group
		Given the user has added at least one query item
		And the user has added a content group
		And all the fields are filled in 
		And the user clicks the "Save" button 
		Then the changes are saved in the database 
		And the user is returned to the Query Group list page 


	Scenario: Assigned query groups cannot be edited
		Given the query group was saved
		And the query group was assigned to a participant 
		Then the query group can not be edited anymore 
		And there is a warning sign informing the user "This query group can no longer be edited."



 
















