# User roles
The KG knows the following roles for users which can be applied either on the global level (for the whole graph), for 
a specific space or for a specific instance (we call those different levels *scopes* in the following).

## Admin
The administrator is allowed to execute any functionality for the defined *scope* (global, space or instance).

## Owner
The owner of a *scope* is allowed to do all the "daily work" - read, write, release and maintaining the space information

## Editor
An editor is able to read and write instances in the *scope* but NOT to release them.

## Reviewer
A reviewer is able to only read instances in the *scope* but neither to create nor to manipulate them

## Consumer
A consumer is only allowed to read released instances of the *scope*