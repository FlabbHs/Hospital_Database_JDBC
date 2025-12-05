# HOSPITAL DATABASE JDBC PROJECT

This project is a simple Java application that connects to a MySQL hospital database using JDBC. It provides a console menu that allows the user to run basic database operations and test commit/rollback functionality

### Project Structure
```text
Hospital_Database_JDBC-main/
├── src/
│   ├── AppointmentTable.java
│   ├── BillTable.java
│   ├── DB.java
│   ├── Main.java
│   ├── PatientTable.java
│   ├── create_and_populate.sql
│   └── db.properties.example
│
├── create_app_user.sql
├── test_database.sql
├── JDBC.iml
├── .gitignore
└── out/              
```

### Description of Key Files
#### Java Source Files (src/)
- Main.java

  Entry point of the application. Contains the console menu and program loop.
- DB.java
  
  Handles JDBC connection logic. Reads credentials from db.properties.
- PatientTable.java, AppointmentTable.java, BillTable.java

  Contains CRUD operations and SQL interaction for each database table.
- Db.properties.example

  Example configuration file showing how to store database connection settings.
- Create_and_populate.sql

  SQL script used to create all tables and insert initial data.
#### SQL Setup Files (root folder)
- Create_app_user.sql

  Creates the MySQL user and assigns privileges.
- Test_database.sql

  Creates sample tables and test data (alternative to create_and_populate.sql depending on workflow).


###  Setup
1. Start MySQL
2. Create the application user:
   
  ``` SOURCE create_app_user.sql;```

3. Create tables and load data:
   
  ```SOURCE src/create_and_populate.sql;```

4. (Optional) Run test_database.sql


### Configuration

Copy the example properties file:

``` cp src/db.properties.example src/db.properties ```

Then edit db.properties to match your MySQL username, password, and database name

### Running the Program
- Open the project in IntelliJ or another Java IDE
- Ensure the MySQL server is running
- Ensure db.properties is correctly configured
- Run Main.java
- Use the console menu to interact with the hospital database

Features
- View, insert, and update hospital records (patients, appointments, bills)
- JDBC-based MySQL connectivity
- Transaction control using manual commit and rollback
- Database setup scripts included
- Modular table-specific classes

