# Timeslot - (Short) Whitepaper

## General Definitions
This project is based on the assumption that time can be tokenized as any other tangible asset.
In everyday life many activities can be seen as an exchange (or trading) of a certain amount of time.  Sometimes this is obvious… other times, to see it clearly, we need a change of perspective, to look at things from another point of view.
In general, a good rule could be to see if we use something for a period of time.
Some examples:
- Rent: a car, a room, a suit, a storage space
  We are taking the ownership of  goods for a *limited period of time*
- Professional services: medical or professional advice, mechanic, baby-sitter, plumber…
  In this case we pay for the *time* that an expert dedicates to us
- Entertainment: cinema, theaters, concerts, shows
  we pay to be entertained for a *certain amount of time*

We have to make some assumptions:
- No one can own a Timeslot. You can only spend it.
- Timeslots are not reusable… You can’t experience the same moment twice
- Time tokenization makes sense only if it is associated with another entity.
  A token that represents "2 hours" is useless (at least for our purposes). But we can add a meaning if we associate those "2 hours" with something or someone: a car (rent a car for 2 hours), a professional (2 hours of consulting), a seat in a cinema (we can stay and watch a movie for 2 hours)
- Sometimes the same Timeslot (the same 2 hours) can be shared by many people.
  Consider a theater: many people can spend the same 2 hours in the same place watching a show.
- A Timeslot can be associated with a geographical position. Let’s take the example of a parking space, for instance. We choose this space to provide ultimate convenience for our personal needs at that time.

The purpose of the Timeslot project is to create a framework that can manage all these situations (See the user guide and the Use cases for further details).
At the moment this project only exposes a REST API that can be used as a service in other software.
In the (near) future we are planning to create a User interface for basic administration tasks and later we will provide an end users oriented interface.

## Timeslots
A Timeslot is an asset that represents a time interval.
It can be defined using
1.  a start and an end time or a start time and a duration: if the time period is defined and known.
2. only a duration: if we are representing just an amount of time but it’s not known when it will start
To describe a Timeslot we need some metadata:
- Name -> text
- Code-Id -> text (max length 7 chars)
- Description -> text, optional
- Start validity -> timestamp
- End validity -> timestamp, optional if duration is specified
- Duration -> optional
- Price -> a decimal number  (can be zero) that represents the price in Algo
- Location -> specify a geographic place.
- latitude -> optional
- longitude -> optional
- name -> madatory
- address
- type -> the Timeslot type: Ticket - Receipt

These metadata are stored directly in the Algorand blockchain.
An Algorand asset is a good candidate to represent a Timeslot that, because of this, also inherits the following metadata :
- Unit Name
- Asset Name
- Total supply
- Decimal (in this case it will always be 0)
- Url


### Timeslotpair
A Timeslotpair is a pair of two Timeslots (actually they are two Assets with the same metadata). The first one can be considered a ticket. The second one a receipt.  The following rules apply:
- Only the Ticket can be purchased
- Only the Ticket can be spent
- When you spend a Ticket you receive a Receipt (it’s an atomic transaction)
- You can exchange/sell your receipt with others

## Operations available on Timeslots
### Administration
**Users management**
- create a new user
- [ToDo] delete a user
- [ToDo] modify a user

**Timeslots management**
- list the available/archived Timeslots
- retrieve the available or the archived Timeslots
- create a new Timeslot
- create a new Timeslotpair
- [ToDo] destroy a Timeslot
- [ToDo] destroy a Timeslotpair
- send a Timeslot to a user
- list of Timeslots spent by a user
- This function is useful to check

### User
- list the available/owned Timeslots
- get the details of a Timeslot  
- take an amount of Timeslots
- spend an amount of Timeslots 
- [ToDo] get account info
- List of the transactions relative to an asset



