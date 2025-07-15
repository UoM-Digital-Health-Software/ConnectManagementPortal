Feature: Query Group list


Scenario: Viewing the query group list page
	Given the user is on the query group list page
	Then the user can see a list of all the created, unarchived query groups
	And there is a button to archive a query group 
	And there is a button to duplicate a query group 



Scenario: Archiving already assigned query group 
	Given the user clicked "Archive" button next to a query group 
	And the query group is currently assigned to a participant
	Then a message is displayed informing the user the query group cannot be assigned 
	And the query group is not archived 


Scenario: Archiving a query group that is not assigned to a participant 
	Given the user clicked "Archive" button next to a query group 
	And the query group is currentnly not assigned to anyone 
	Then the query group is archived 
	And the query group is no longer in the list 



Scenario: Duplicating a query group  
	Given the user clicked "Duplicate" button next to a query group 
	Then the query group is duplicated
	And the page with the Query Builder is opened 
	And all the content from the original query group is copied over 



Scenario: Viewing the archived query group list 
	Given the user is on the archived query group list 
	Then the user can see all archived queries 
	And there is a button to unarchive each query group 


Scenario: Unarchiving already archived query group 
	Given the user is on the archived query group list
	And the user clicks the "Unarchive" button 
	Then the query group is unarchived 
	And the query group disappears from the list of archived query groups 








