Feature: Assigning a query group to a participant  


Scenario: Accessing a Add Query Group modal for a participant  
	Given there is at least one already created query group
	And the user navigated to the list of participants
	And the user clicks Queries button for one of the participants 
	Then a modal is opened 
	And there is a dropdown with all the existing query groups 
	And there is "Assign" button 
	And there is a list of already assigned queries 


Scenario: Adding a query group to a participant  
	Given the user has opened the modal for assigning query groups to participants 
	And the user selected already existing query group to a participant 
	And the user clicked "Assign" button
	Then the query group is assigned to the participant 
	And the query group is the list of assigned query groups 



Scenario: Deleting a query group from a participant   
	Given the user has opened the modal for assigning query groups to participants 
	And the user already assigned a query group in the past 
	When the user clicks "Delete" button next to an assigned query group 
	Then a prompt is displayed "Do you want to remove the content from the user's device"



Scenario: Aggreing to delete the content from the user's device  
	Given the user is deleting a query group from a participant 
	And and the user has agreed to remove the content from the user's device
	Then the content from the user's device is deleted by unassining it in the DB
	And the query group is unassigned from the participant 



Scenario: Not aggreing to delete the content from the user's device  
	Given the user is deleting a query group from a participant 
	And and the user has not agreed to remove the content from the user's device
	Then the content from the user's device is not deleted 
	And the query group is unassigned from the participant 




