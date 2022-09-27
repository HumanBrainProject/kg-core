# The API

The EBRAINS KG Core API allows to programmatically interact with the EBRAINS Knowledge Graph and is therefore the most
important interface of the ecosystem. 

### Versioning
The EBRAINS API is versioned to be able to ensure backwards compatibility. However, for practical reasons, only **two** stable versions
will be maintained at the same time: The **current** as well as the **previous** version which will be maintained for those clients
that haven't been able to migrate yet. Additionally, the KG Core can provide a **development** version of the API which allows early and prototypical integration. 
However, this version is not intended for productive use and is therefore not regarded to be *stable*.

Accordingly, actions have to be taken that before a version is bumped (and therefore the **previous** version is going to be removed), that all clients have been migrated 
to the **current** version (which will become the previous after the bump).

#### Example: 
 
**previous version**: v2  
**current version**: v3  
**development version**: v4  

Now, the development version became stable and therefore, the version is going to be bumped. So the following actions have to be taken:
- Analyze if still any activity is going on on the v2 or if a client is known still using this endpoint. This can also involve a communication to the integrated clients by mailing list / support channels, etc.
- If there are still clients using v3-beta, efforts have to be taken to get them migrated as quickly as possible
- Once all clients are migrated (or all possible efforts have been taken to do so), v2 is removed, v3 is tagged to be "previous" and v4 is tagged to be "current". 
- Eventually, a v5 can be introduced as a development version

Please note, that the terms **previous**, **current** and **development** are not going to be reflected in the API endpoints. It's a convention for handling organisational aspects and the endpoints will always contain their specific version number.

### Grouping
The various endpoints are grouped by their purpose and the resources they handle. Almost all endpoints are meant to be executed with an authentication header (see [Authentication](authentication.html)). 
Accordingly, some endpoints will either return different results depending on your access rights or they will not be accessible at all. To interpret the response, please have a look at the [openAPI specifications](openapi_specs.html)

