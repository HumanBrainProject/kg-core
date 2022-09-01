# Permissions

The EBRAINS Knowledge Graph allows a very fine-granular way of managing permissions. To organize the various access 
permission aspects, the KG knows the following concepts:


## Spaces
"Spaces" are logical units which define the access rights for individuals or groups of users. 

By default, a space is a configurable area for which the permissions have to be explicitly defined (see [authorization](authorization.html)).
Additionally, the KG knows two special spaces:

### "myspace"
The "myspace" is the personal space of an authenticated user. It means that any user who is logged is an administrator 
of its "myspace". From a technical perspective, "myspace" is a dynamic alias resolving to the spacename "private-ID_OF_THE_USER". 
Please note, that the according access rights still have to be configured properly (see [permissions](permissions.html))
However, the API makes this transparent to the consuming instances meaning that "myspace" appears to be a valid space name.

### "review"
The "review" space is a virtual space that combines all instances to which a user is invited (see below). Accordingly, the instances of the "review" space
are located in different spaces in reality.

## Invitations
Users can be invited to individual instances. This means, that although they are not having any permissions on the 
space of the given instance at all, an invitation allows them to access it. Because usually, giving access to a single instance
is not sufficient in a graph database, the EBRAINS Knowledge Graph invitations are valid for the so-called "scope" of an instance

### (Invitation)scope
The scope of an instance describes the instance itself as well as its directly related resources. The 
scope is calculated based on a specific set of "queries" which describe the dependencies between the entities based
on specific use-cases (such as the combination for producing the aggregation result of the KG Search).
The invitation scope is calculated at invitation time and can be updated via the corresponding API endpoint either regularly or on demand.