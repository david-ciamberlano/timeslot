# User Guide

## Index
- Prerequisites
- The application.properties file
- First run
- Creation of new Users
- Example

## Prerequisites
Timeslot is distributed as a zip that contains a single executable jar, an "application.properties" and a H2 DB file.
To execute the application, you need to have java 11 (or later) installed.
Check if you have the correct version by typing:
```
java -version

openjdk version "15.0.1" 2020-10-20
OpenJDK Runtime Environment (build 15.0.1+9-18)
OpenJDK 64-Bit Server VM (build 15.0.1+9-18, mixed mode, sharing)
```

**Important**: 'admin' and 'archive' accounts must have Algos in their wallet to work properly
You can send Algo from the admin account to other users using the following API:

`POST /admin/v1/algo/send/{amount}/to/{user}`

## application.properties file
This file contains the configuration parameters for the application.

**NOTE**: H2 DB is recommended only for testing purposes
You can use another existing DB by changing the configuration of the datasource in the "application.property" file

```properties
server.port=9696

#algorand server & indexer
algorand.algod.address=https://api.testnet.algoexplorer.io/
algorand.algod.port=443
algorand.algod.api-token=

algorand.indexer.address=https://api.testnet.algoexplorer.io/idx2
algorand.indexer.port=443

#datasource configuration
spring.datasource.url=jdbc:h2:file:./algodb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=sa
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.hibernate.ddl-auto=validate

# Enabling H2 Console and Custom H2 Console URL
spring.h2.console.enabled=true
spring.h2.console.path=/h2

# Swagger
swagger.host.url=http://localhost:9696

#Timeslot Properties
timeslot.secret.key=change-me-please
timeslot.admin.user=admin
timeslot.archive.user=archive

```


## First run
Unzip the packet in a folder and then use the following command to run the application
```
java -jar timeslot-0.2.jar
```

"algo.mv.db" is a default database, containing only the "init" user (which is only used to create the other users and can be safely deleted after the initialisation).
NOTE: The default password for "init" is "admin".

Alternatively, you can let the application automatically create an empty DB.
After the setup, a new file will be created: "algo.mv.db" (if you are using the H2 DB)
In this case, the first time you use the Timeslot application, you have to create the "init" user manually. You can do that using the following SQL script (the password is: init)

```sql

INSERT INTO users (username, password, enabled) VALUES
('init','$2a$10$Y1OQ3JCE05olIuVER0i7peeJSccHr4P2e2EIslecXat6tqmxyGd46', TRUE);

INSERT INTO authorities (username, authority)
values ('init', 'ROLE_ADMIN');
```

You can also manage to the H2 DB using the administration console: [http://localhost:9696/h2/](http://localhost:9696/h2/)


## Creation of new Users
In order to test the application, you need at least three users: admin, archive and a "normal" user.
You can create these users using the Swagger Interface:
`http://localhost:9696/swagger-ui.html`

In order to do any operation, you must log in. In  the swagger interface you can click on the small padlock on the right side of the page and enter the username and password of the init user  (Username:init -  PW:admin) or of any other administrator.

You can now create the 2 most important users: admin and archive (you can change these names in the configuration file).

### Admin user
The admin user is the system administrator. He can create new Timeslots and do many other useful administration tasks.
You can create it using the following function:
`POST: /admin/v1/user`
You can click on the "Try it out" button and use the following json in the "request body" field:
```json
{
  "username": "admin",
  "password": "<your-password>",
  "algoAddress": "<existing-account-address-or-empty>",
  "algoPassphrase": "<existing-account-passfrase-or-empty>",
  "administrator": true
}
```
If you leave the algoAddress and algoPassphrase fields empty, the app will create a new account. This is not recommended for the Admin account but is fine for the others.
The admin account must have Algos in his balance. So a better approach is to use an existing account you can manage with an external wallet
**NOTE**: The admin account must have Algos

### Archive user
Archive is a special administration account which only purpose is to receive all the assets spent by users.
Every time a user spend a Timeslot, it goes in the archive account wallet and stay that forever (or at least until the administrator destroys it).
You can create it exactly in the same way of the admin. Just use "archive" as username.
```json
{
  "username": "archive",
  "password": "<your-password>",
  "algoAddress": "<existing-account-address-or-empty>",
  "algoPassphrase": "<existing-account-passphrase-or-empty>",
  "administrator": true
}
```


### End user
Finally, you can create the end users.
Again the procedure is exactly the same, but  you must set the "administrator" property to false.
```json
{
  "username": "archive",
  "password": "<your-password>",
  "algoAddress": "<existing-account-address-or-empty>",
  "algoPassphrase": "<existing-account-passphrase-or-empty>",
  "administrator": false
}
```

## Example - Professional advice
A professional wants to offer online advices.
He would like to schedule a  few slots each week, each lasting one hour, dedicated to this activity .  
He wants to receive only one client at a time, so he needs a solution that does not allow more than one people to book the same time slot.

**NOTE**: in the video folder there is a video showing these steps (timeslot_2-use-case-1.mp4)

### Step 1 - [Admin] Create new Timeslots
**NOTE**: you have to login as "admin" in the swagger UI (click on the small padlock on the right).
You can create a new Timeslot for each advice hour, using the rest api:
`POST /admin/v1/timeslots`

In the body he can use a json object similar to the following:
```json
{
    "defaultFrozen":false,
    "unitName":"JS01-01",
    "assetName":"John Smith 20-01-2021 11:00",
    "assetTotal":1,
    "assetDecimals": 0,
    "url":"www.example.com/bc/ts/PROS20013",
    "timeslotProperties": {
        "startValidity":"1610276400",
        "endValidity":  "0",
        "duration":1,
        "timeslotUnit":"HOURS",
        "price":25,
        "description":"John Smith schedule 20-01-2021 11:00 to 12:00",
        "timeslotLocation": {
            "name":"Colosseum Office",
            "address":"Piazza del Colosseo, 1, 00184 Rome",
            "latitude":"41.890405350990484",
            "longitude":"12.492181398359188",
            "hasCoordinates":"true"
        }
    }
}
``` 
If the Timeslot has been created succesfully, you obtain a 201 http response.
You can create more Timeslots simply changing some parameters in the json.

### Step 2 -  [Admin] Check the timeslot created
You can check Timeslots created in the step 1 using the REST Api:

`GET /admin/v1/timeslots`

The f parameter could be "available" or "owned".  For this Use case you can use "available" and check if the new created Timeslots are in the List returned by the REST API.

### Step 3 - [User] take the list of the available Timeslots
Authenticate as a user in the Swagger UI.
You can obtain the list of the available Timeslots by using the REST api:

`GET /user/v1/timeslots`

You can use the "available" keyword for the filter and you can put the prefix of the unit Name in the prefix parameter, in our case "JS".
Take note of the Asset Id of the Timeslot you are interested in.

### Step 4 - [User] Take a Timeslot
Using the Asset Id from the step 3, we can Take (buy)  a Timeslot.

`POST /user/v1/timeslots/{id}/take/{amount}`

You must put the Timeslot Id in the "id" field and how many Timeslots you want to buy in the "amount" field. In our case we can only buy 1 Timeslot.

### Step 5 - [User] Check owned Timeslots
You now have a new Timeslot in your wallet. You can check this as in the Step 3 but specifing the "owned" keyford for the filter.
You can log-in as Admin and check (see Step 2) that the timeslot is no longer available (amount: 0).

### Step 6 - [User] Spend the Timeslot
You can spend the Timeslot just befor the start of the advice session (to certify the you are the user who bought the Timeslot):

`POST /user/v1/timeslots/{id}/spend/{amount}`

You have to specify the Timeslot id (taken in the step 3), the amount (1 in this case) and a note to the admin.
If the transaction is successful you will receive the http 202 code.

### Step 7 - [Admin] Check the spent Timeslot
The administrator can check if you spent the correct timeslot before starting the advice session:

`GET /admin/v1/timeslots/{id}/transactions`

This REST api returns the list of all the transaction related to the specified Timeslot (asset).
To filter the list, you can specify the following filters: sender username, receiver username, note prefix.



