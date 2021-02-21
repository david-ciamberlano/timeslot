# Timeslot - some use Cases

## Use Cases
### UC1 - Use Timeslots to manage tickets of a parking.
**Description**
We want to manage a parking space. Just to simplify, suppose to have only one available space.  Suppose also to set a minimum unit of time a user can purchase, say 15 minutes (this is quite frequent in public and private parks).
Usually a driver must pay in advance the time he parks his car and he can eventually extend that period later.
The driver wants to park his car in our space. He plans to left after 45 minutes. After 40 minutes he realizes he need 30 more minutes.

**Preconditions**:
The system administrator create a new Timeslot specifying the following information:
- duration = 15 minutes
- location = <location>
- price  for each Timeslot

**Flow**
1. The driver buys  a certain amount of Timeslot (say 10)
2. When the parking start, the driver  spends (SPEND) 3 Timeslots (because initially he plans to park for 40 minutes) specifiyng the registration number of his car
3. A ticket inspector can if the driver paid for the parking getting the list of the transaction for that particular Timeslot. In this way he can easily find that the driver, at some time and for a specific registration number, spent 3 Timeslots.
4. The driver can extend is parking time by 30 minutes spending 2 more Timeslots before the end of the previous period

### UC2 - Professional advice
**Description**
A professional wants to offer online advices.
He would like to schedule a  few slots each week, each lasting one hour, dedicated to this activity .  
He wants to receive only one client at a time, so he needs a solution that does not allow more than one people to book the same time slot.

**Prerequisites**
The professional creates a Timeslot for each hour working day (after each day he can destroy the expired Timeslots to save resources).
The timeslot has the following properties:
- start-time
- end-time
- price

**Flow**
1. The user get the list of available Timeslots (one for each consultancy hour). Then he can buy the desired Timeslot. He owns now the ticket and can be sure no one else can take the same time.
2. Before starting the consultancy, he can spend the corresponding Timeslot to certify he met the Pro.

### UC3 - Tickets for an Event or a show
**Description**
A company organises a series of conferences and wants to sell tickets for them. The place  can house a maximum of 120 people.
People who attend a conference will receive a receipt that certify their presence. They can keep it for later uses.

**Prerequisites**
Organizers create a Timeslotpair for each concert with a fixed price and a total amount of 120 (because 120 is the maximum capacity of the place).

**Flow**
1. User get the list of the available Timeslotpairs. He can then buy one or more of them. He will receive the corresponding amount of Tickets in his wallet .
2. Before the start of the event, he can spend/validate his ticket to gain access to the venue and immediately after he will receive a receipt directly in his wallet.
