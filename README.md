# Stepper
Developed as part of Aviad Cohen's Java course at the Academic College of Tel-Aviv Yaffo. 

The Stepper is a workflow\pipeline system that enables assembling different	scenarios (called flows) from common components (called steps), including executing them and	producing required results.

This is a continuation of the [Stepper-Desktop](https://github.com/IgalKa/Stepper-Desktop) version. 

The project now includes components that manages users and permissions, serving multiple clients concurrently and collection information and statistics on their progress.

The system is developed in Java. It includes practice with multithread concerns, client-server architecture, JSON serialization and much more.

# Overview
A step is a piece of independent logical unit that is able to carry out a specific action (e.g. deleting files, exporting textual data, sending http request).

Each step has its own inputs that are required for the step to execute its action, and outputs that are produced after the execution.

Given a set of steps, we can make a "connection" between the different steps so that the output of one will serve as the input of the other. The collection of steps connected together in this way is called a flow.

The Stepper system allows to take the different step definitions and combine them into flows to produce different work flows (models)
we can then proceed to exectue the flow, activating the execution of each step within the flow.

The project includes two different clients(user client and an administrator client) and a server (powered by Tomcat).
While users can execute different flows and see their results, the admin is responsible for the flows that are loaded into the system, managing users permission to use those flows and more.


# Features
* The flows definitions are loaded into the system by the administrator by an XML file 

* **Automatic mapping** - the system will automatically map the outputs to the various inputs of the steps in the flow

* **Custom mapping** -  define custom input/output mapping via the XML file

* **Name aliasing** - change the name of a step/input/output to distinguish them in case there is more than one in a flow.

* **Initial values** - define an initial value for a step's input

* **Continuation** - use the information and values ​​produced in a previous flow execution in another flow's execution. This ability allows an immediate and fast connection between one flow execution and another.

* **Flow execution** - the user can choose a flow, provide all the required inputs, and execute the flow

* **Asynchronous execution** - the ability to execute multiple flows concurrently.

* **Flow execution data** - view all the data and information produced as the flow executes (updated as the flow progresses)

* **Flow execution history** - view all past execution including all the executions's data.

* **Execution rerun** - by selecting a past execution, it is possible to rerun the same execution (same inputs) immediately

* **Login/Logout** - users can login and logout

* **Chat** - users can talk to each other through a chat-room that is available to all users who are connected to the system

* **Roles** - the admin can define and assign roles in the system. When a role is assigned to a user, it grants the user permission to use the flows that are defined in the role.

# User Client

#### Login screen
* Enter your username to signup/login to the system.

#### Top section of screen
* View username and assigned roles.
* Enable/disable animation by the radio button to the right.
* Select the system style from 3 different options.
* Logout of the system

#### Flows dashboard screen

* This screen allows the user to view all the available flows that he has permission to run (based on his assigned roles).
* View the flows full definition information by selecting a specific flow from the table.
* Select the desired flow for execution and click on "Execute flow" to proceed to the execution screen
  
![Definition screen](https://github.com/IgalKa/Stepper/blob/master/gifs/Definition-screen.gif)

#### Flows execution screen
* This screen allows the user to execute the selected flow.
* At the top are the mandatory and optional inputs of the flow, the flow will be ready to execute when all the mandatory inputs will be filled.
* Left click on an input to insert data into it.
* Right click on an input to view or delete its current data.
* When the flow is ready, click on execute to run the flow.
* The execution progress will update at the lower part of the screen.
* It is possible to click on each completed step in the table to view its specific execution data, as well as the full flow execution progress.
* Once the flow finishes its execution, it is possible to rerun the flow or apply continuation (if available) by the respective buttons that become available.
* Access the chat feature by clicking on the chat button in the bottom left.
  
![Execution screen](https://github.com/IgalKa/Stepper/blob/master/gifs/Execution-screen.gif)

#### Executions history screen
* This screen allows the user to view all his past completed executions (or all completed executions in the system if the user is a manager).
* Select a flow in the table to view its full data execution (it is possible to select each step of a flow).
* It is possible to rerun the selected flow by clicking on the rerun button.
* If the flow have a defined continuation it is possible to apply the continuation by clicking on "Continuation options".
* Access the chat feature by clicking on the chat button in the bottom left.
  
![History screen](https://github.com/IgalKa/Stepper/blob/master/gifs/History-screen.gif)

#### Chat
The chat is available in the flows execution or executions history screens.
![Chat interface](https://github.com/IgalKa/Stepper/blob/master/gifs/Chat-interface.gif)



# Administrator Client

There can only be one active Administrator client at any given time.

#### Top section of screen
* Load XML file by clicking on the button to the left.
* Select the system style from 3 different options.

#### Users management screen
* This screen allows the adming to view and manage the currently connected users.
* The list to the left displays the usernames of all connected users.
* Click on a username to view the selected user's information in the right section of the screen.
* Edit the assigned roles of the selected user by checking/unchecking the checkboxes near the wanted roles names and clicking the save button (there is also an option to make a user a manager).
  
![Users screen](https://github.com/IgalKa/Stepper/blob/master/gifs/Users-screen.gif)

#### Roles management screen
* This screen allows the adming to view and manage the roles in the system.
* The list to the left displays the names of the currently defined roles in the system (There are two predefined uneditable roles in the system, All Flows and Read Only Flows).
* Click on a role name to view the selected roles's information in the right section of the screen.
* Edit the flows that the selected role grants permission to use by checking/unchecking the checkboxes near the wanted flows names and clicking the save button.
* Add new roles to the system by clicking on the new button and filling the required information.
* Delete an existing role by selecting it in the list and clicking the delete button.
  
![Roles screen](https://github.com/IgalKa/Stepper/blob/master/gifs/Roles-screen.gif)


#### Executions history screen
* This screen allows the admin to view all completed executions in the system.
* Select a flow in the table to view its full data execution (it is possible to select each step of a flow).

For demonstration see user's client execution history section.


#### Statistics screen
* This page allows the admin to view the statistics of past executions.
* View how many times and how much time on average it took for each step/flow to execute.
* View the statistics data in graph view by clicking on the buttons at the bottom.
  
![Statistics screen](https://github.com/IgalKa/Stepper-Desktop/blob/master/gifs/Statistics-screen.gif)


# Server
The server is powered by Tomcat and utilize different servlets to get HTTP requests from the users/admin and send back responses.

The Stepper Engine and most of its logic is located within the server.

## Authors

This project was made by [Igal Kaminski](https://www.github.com/igalKa) & [Michael Shuster](https://github.com/MichaelShuster1)
