# Concepts and conventions

## Multi-source contributions
One of the main concept leading the development of the EBRAINS Knowledge Graph is the multi-source contribution:

The EBRAINS Knowledge Graph wants to embrace the fact that

- information is ingested from various other systems
- information is linked together tightly or loosely at ingestion time
- multiple users are operating on top of the graph by adding partial information, correcting bits and pieces over time and have discussions about the correct terminologies
- creation of common understanding and decisions on ontological standards are achieved collaboratively and iteratively

Therefore, not only the tools in the KG ecosystem (e.g. KG Editor) but also the underlying core has been designed to simplify the contribution of multiple parties to a shared state of knowledge. When interacting with the Knowledge Graph, a user (or a script) follows the "create or contribute" approach: When uploading a payload to the KG, the system checks if it contains a representation of this instance already (e.g. by the external identifier the payload provides). If not, a new instance is created, otherwise it is regarded to be a contribution. Essential for the contribution is, that the information is stored separately from the instance representation and "by user". This means, that multiple users can contribute to the same instance but the information they provide is held separately. Obviously, if the same user updates information, the previously existing contribution is replaced.

#### Documents vs. Instances

Because of this, the Knowledge Graph internally distinguishes "documents" from "instances": documents are contributed payloads by user whereas an instance represents a real entity with a life-cycle and which is composed of the information from one or multiple documents.

#### Inference & Alternatives
The component actually deciding which values are taken into account for building the instance is called "inference". We follow the approach that the most recent change is taken into account for the actual value of the instance whereas the other non-picked values are represented as so-called "alternatives" which can be consumed via the API (e.g. to be shown in the KG editor).

## Staging / Releasing
The information in the Knowledge Graph is always in the flow: new information coming in / being updated / being removed automatically, semi-automatically and manually. Since the EBRAINS Knowledge Graph is treating scientific data where it is absolute key that the information is validated and consistent, the Knowledge Graph knows the concept of stages - which mainly means that it provides a "releasing" mechanism:
The releasing of an instance means that its current state is copied into another database which will not be affected by following updates until the instance is re-released. Tightly integrated into the KG Editor, this process is well-suited for quality assurance.

#### Automated releasing
Although releasing is a very helpful and important tool, it can add a lot of overhead in terms of manual work or API calls. The KG therefore allows to define individual spaces with "automated releasing" where changes are released immediately. This is e.g. useful if a trusted and quality ensured automated script delivers non-disputable information in regular intervals (e.g. an index of a file system).

## Multi-index
Sometimes, a single database can not support all the requirements of a specific use-case because they are optimized for specific concepts. The EBRAINS Knowledge Graph therefore addresses fundamentally different needs with different indices whilst hiding this fact in unified APIs. One of those cases is the separation of semantic search and spatial search (e.g. of instances by their location in 3D space). The provided information is therefore registered in different databases and reconciled at query time.

## Query specification
The EBRAINS Knowledge Graph provides a descriptive query mechanism which allows to specify which parts of the Knowledge Graph (instances, properties, traversals) should be queried and which also allows to restructure the returned JSON (flattening, merging, etc). Its concept and structure is very close to the one of GraphQL but brings a few additional advantages:

- Support of semantics: The declaration of fully qualified semantic keys can be guaranteed 
- Reflection: The specification itself is a JSON-LD. The reflection on it is very simple - this also allows to close the feedback-loop to the tools (e.g. the KG Editor can show which properties are used by which queries and therefore advice users which values should be added).
- Abstraction: The abstraction layer the query builds combined with the reflection capabilities allows to give hints to the author of the query (e.g. because a field is not available anymore / a low or decreasing number of instances have defined the queried values, etc.) and to support data structure migrations inside the KG without needs for adaption on the clients side.

## Immediate & eventual consistency

As often the case, there are trade-offs when thinking about consistency: While eventual consistency with asynchronous population of databases is a very nice concept for scalability, some clients are hard or almost impossible to build with a satisfying user experience when it is not yet clear when a change will actually be persisted in the underlying database. The EBRAINS KG tries to meet this problem with a dual-consistency approach: With two modes (synchronous and asynchronous), the clients can chose if they need an immediately ensured consistency (this is e.g. the case for the KG Editor) or if they can live with an eventual consistent database (typically regularly running cron-jobs / read-only clients).

Controlled by permissions for the KG core clients, the immediate consistency mechanism (which is more expensive) can even be disabled unless there is a valid need expressed by a client.

## Dual-Authentication

Calls to the KG always have to be authenticated. This authentication actually consists of two different authentication patterns - making the API expecting two headers:

- Required: once the standard "Authorization" header for the user authentication
- Optional: once the custom "Client-Authorization" header for the client authentication

The additional client authentication is applied to configure managed client use and contextualization. It is intended for clients with their own back-end which can profit from storing their own meta data information in the graph. Also, this concept allows us to restrict access not only by user but also by the client by which the user accesses the Knowledge Graph. Imagine a user who has write access to a specific space "A". If the user opens an instance of space "A" in the Knowledge Graph Editor, the write access will be granted (since the client "KG Editor" does have write access too). If on the other hand, the same user accesses the same instance - e.g. via the KG Search UI, only read access will be granted because the "KG Search UI" client only has read access. This combination of the access rights does allow the KG to keep track of which client can do what on the Knowledge Graph.

#### Special case: Automated interaction / client-side access restriction
The standard case assumes that there is a user who interacts with the system (e.g. via a web UI) and can provide its access token from a browser session. But there are also several cases where an automated interaction should take place (e.g. scripts operating on top of the data) or the need to relax the access restrictions (e.g. for anonymous access of a specific set of data). For these cases, all registered clients are having a "service account" which can act as a normal user and therefore also provide its token as a user token. Needless to say that when exposing data collected by this service account, it's the responsibility of the client to take the appropriate measures of ensuring that the granted rights are not abused (only the intended data is exposed, only restricted manipulation operations are possible, etc.) since this is the equivalent of sharing the access rights given to a client.

The API itself allows the following header combination to achieve the various ways of authentication:

**"Authentication" only:** This is a direct access of a user (which could be a service account too). All permissions the user has are granted. This is e.g. well-suited for Jupyter notebooks or similar.

**"Authentication" and "Client-Authentication":** The KG Core is accessed by the user (defined by "Authentication") in the context of a client ("Client-Authentication"). The "Client-Authentication" expects a bearer token which can be obtained e.g. by exchanging the client-id and client-secret directly with the authentication system. This is the recommended way of authenticating against the KG Core since it doesn't expose any client specific credentials other than short-lived access tokens.

For convenience, we also provide the following two convenience mechanisms to the two authentication possibilities above. Please note, that they are intended to further simplify the interaction with the KG core. We highly recommend to use the standard authentication mechanisms for external production-level integrations since the following mechanisms potentially expose the client credentials to the KG Core.

**"Client-Id" and "Client-SA-Secret":** The same as "Authentication" and "Client-Authentication" to authenticate the service account as both, the client and the user. This means that you have all the rights of the client and receive contextualized information for this client on the KG Core.

**"Authentication", "Client-Id" and "Client-Secret":** The same as "Authentication" and "Client-Authentication" - but this time, the client bearer token has not to be requested by the client itself but this is rather done by the KG core.

## Contextualized API

As stated above, the dual authentication allows to clearly identify a client. But it's not only about identification: Some functionalities are/can be client specific - such as the definition of meta information: Client A can e.g. specify its own "human readable" label for a specific property. The KG core endpoints therefore take into account the client information to return client-specific settings and therefore make the API contextualized depending on "who is asking".

## Authorization / permission management

The EBRAINS Knowledge Graph knows two granularity levels of permissions:

### Spaces

The EBRAINS KG is organized in so-called "spaces". Spaces are wrapping permission groups together - this means, that a group of users can be given rights (such as write / read) for an individual space. A space does not restrict the instances it can contain - so any payload (and therefore any type) is possible, although it is good practice to organize similar types in the same space to simplify the organization of responsibility. If a user belongs to multiple spaces, the combination of all readable instances will be returned.

#### Public spaces
A space can be marked to be "public" through the API. If a space is public, its released instances can be read by any successfully authenticated user. On the other hand, if a space is "protected" (the default at creation time), its released instances can only be read by users which have an explicit "consumer" role defined.

### Invitations
Next to the spaces, it is possible to give individual rights on instance levels. This is achieved by so-called "invitations". A user with invitation rights of a specific space can invite (or withdraw an invitation from) other users to the instances in it. Since usually, it doesn't make sense to only have access to a single instance in a graph context, the invitation is not only valid for the instance selected but also for a deterministic scope of related instances. This scope is evaluated dynamically based on the available stored queries.

### Collab permissions
The EBRAINS KG supports the definition of role mappings according to the underlying authentication system. In our context, we are also making use of the collab permissions - so a user who is member of a collab which has an assigned KG space can interact with the space (admins -> owner, editors -> editor, viewers -> reviewer). Please note that all collab spaces are prefixed with "collab-" so it's unambiguous which space we're talking about.

### Available permissions
The EBRAINS KG knows quite fine-granular permissions. Next to the "standard" read/write/create/delete permissions, here is a selection of some more special ones:

- Suggest: A user with suggest permissions (and without write-permissions) is allowed to contribute to an instance - the ingested value is not applied immediately though, but is stored as an "alternative" (regardless of the applied decision logic of the inference mechanism). The suggested value then can be queried and/or presented (e.g. in the KG Editor) and accepted by a user who has write access to the space/instance. 
- Invite for review: As mentioned beforehand, users can be invited to instances without being part of the authorized user group of the space. This permission allows a user to invite another user with "read" access only.
- Invite for suggestion: Similar to "invite for review" but with the possibility to give "suggest" rights to the invited user. Please note that the EBRAINS KG does not provide "invite for write" permissions on purpose. This is to ensure that users with access rights to individual instances can not manipulate entities without the requirement of a review by a user with space permissions
- Execute query / execute sync query: Defines, if a client is allowed to execute queries or even synchronously execute queries for immediate consistency.

[//TODO]: <> (Instance scopes)

[//TODO]: <> (Caching of structural information)

## The normalized JSON-LD
JSON-LD payloads can have different ways of representation. Especially the use of "@context" can be very helpful for re-usability and readability. Nevertheless, to make the access to the semantic information as easy and reproducible as possible and to profit from better machine readability, the EBRAINS Knowledge Graph normalizes these payloads.

One of the main goals is to make the JSON-LD payload as compatible to standard JSON as possible to profit from the various tools existing around it.

It therefore makes use of a combination of ["expansion"](https://json-ld.org/spec/latest/json-ld-api/#expansion) and ["compaction"](https://json-ld.org/spec/latest/json-ld-api/#compaction): The expansion ensures that the defined "@context" is resolved at ingestion time. Since these contexts often are located externally (referenced by URLs), this also means a materialization of their content making the payload resistant against manipulations.

To reduce the payload sizes and to simplify the processing of the payloads, the KG then compacts the payload with an empty "@context". This allows to remove inner constructs like "@value" and flattens parts of the payload.

#### Special case: "@list"

For simplicity, the KG decided to introduce one deviation from the JSON-LD standard: In JSON-LD, arrays are - in constrast to standard JSON - by default unordered. If the order of a list should be kept in JSON-LD, it has to be wrapped with a "@list". Since the KG treats JSON-LD as standard JSON (and therefore keeps the order of arrays), this adds unnecessary complexity and introduces overhead. Therefore, the KG reduces the "@list" elements in the payload and always treats arrays as ordered.

#### Null values

Please be aware, that the KG supports the explicit statement of null values in payloads and maintains them. This is required e.g. for the inference mechanism

## EBRAINS & HBP namespaces

For remaining semantically correct, the EBRAINS Knowledge Graph created - next to the reuse of already existing schemas (such as schema.org and neuroshapes.org), the additional namespaces `https://schema.hbp.eu` and `https://core.kg.ebrains.eu` to reflect internal but also not-yet existing terms which eventually might be integrated into more generic semantic schemas. The main datastructures reflect the openMINDS definitions though and therefore, they are addressed by [https://openminds.ebrains.eu](https://openminds.ebrains.eu)