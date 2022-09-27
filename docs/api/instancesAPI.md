# The Instances API
The instances API is meant for the management of the individual entities and is most probably the most interesting for 
you if you're looking for a way to create or manipulate individual entities. Please note that - although possible by sequential id resolution - 
we highly recommend to use the [Query API](queryAPI.html) if you're looking for a way to consume aggregated meta-data from multiple linked instances.

## Main operations
You will notice that you have the typical API endpoints available allowing you to list, fetch, create and delete instances. 
There are just a few specialities we would like to point out:

### Contribute
You might notice that there is no "update" method. But you can contribute to an instance either via PUT or PATCH.
The reason for this is, that the way the KG is organized, the values you're adding are seen as contributions rather than 
as direct updates on the document (for more details, please see [Concepts and conventions](concepts_and_conventions.html#multi-source-contributions)).
Accordingly, you can either change your complete contribution with PUT (meaning you are removing your contributions for those fields that are not specified in the new payload)
or you can use the PATCH endpoint to only apply changes on individual properties whilst leaving previously committed values intact.

You can assume that in most cases, the PATCH method is what you're looking for.

### Releasing
The releasing operations of the instances allows you to update the "snapshot" of an instance e.g. if it has passed
a quality assurance process. You can either release or unrelease an instance

## ID management
One of the most important parts in a Knowledge Graph is the identifier management. Every instance on the EBRAINS KG
receives a UUID. However, by using the appropriate endpoint, it's possible for the client to self-define this id. This is e.g. 
useful if there are already existing UUIDs available from other systems or if the payloads shall be created in a detached environment before being uploaded.

### Additional identifiers
The EBRAINS KG also allows to specify additional identifiers as long as they are provided in the `http://schema.org/identifier` property.
This means, that any **unique(!!!)** identifier (such as an IRI or similar) can be added to the payload. Because the EBRAINS KG
is able to interpret these identifiers from a semantic point of view, it is even possible to specify links with any of the defined `http://schema.org/identifier` in place.

**Special case custom `@id`**   
Another special case is if you are providing a custom ID in `@id` if you do this (and it is not a EBRAINS KG IRI), the `@id` will 
be interpreted as if it would have been added to the `http://schema.org/identifier` field. This means that the instance will receive
a EBRAINS KG UUID which will be specified in the  `@id` and the previously set value is going to be added to `http://schema.org/identifier` making it available for 
linking other instances

## Upload order of instances
Good news here: The EBRAINS KG doesn't force you to any predefined order of uploading instances. You can upload them
in any order you would like since the system will resolve connections between the instances in a "lazy" way.
