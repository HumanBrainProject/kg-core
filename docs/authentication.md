# Authentication
As described in the [concept section](concepts_and_conventions.html#dual-authentication), the KG Core API supports a dual-authentication. 
One to authenticate the user accessing the KG and one (optional) for the client the user accesses the KG with.

Depending on what you want to do with the EBRAINS Knowledge Graph, you have different options:

## Single authentication
This is the "standard" and most simple way of authenticating - as soon as you are providing a valid 
`Authorization: Bearer YOUR_TOKEN` header with your request which originates from the EBRAINS IAM system, 
you can be authenticated. Please make sure that the token you're using is providing the following scopes to work properly:

- openid 
- email 
- group
- profile
- roles
- team

### Quick start
To easily get a valid token (with the right scopes), you can either go to the KG core swagger UI, 
click "Authorize" and authenticate against the "KG" client (ignore the "Client-Authorization" section), 
or you can log in to the KG QueryBuilder or KG Editor, find the "profile button" on the top right and copy your token. 
If you're using Python, we recommend to check out our Python SDK which has inbuilt support for authentication.

### Access token for non-supervised scripts / server2server communication
If you want a non-supervised script to authenticate against the KG (e.g. to regularly fetch metadata) or you want to 
establish user-independent server2server communication between your service and the EBRAINS Knowledge Graph, you can do 
so by using a "service-account" of an EBRAINS IAM client. 

Please follow the instructions about **Authentication with a client** below. 
Once you have received your access token, you can make use of it the same way as you would if it would be a "real" user.

### About authentication and authorization
Please note, that now that you've been authenticated, this does not necessarily mean that you have access to all the 
metadata available in the graph! If you're looking for additional metadata, please contact [kg@ebrains.eu](mailto:kg@ebrains.eu) 
to request additional rights with the reason and justification (ideally from the e-mail address which is assigned to your EBRAINS account).

## Dual authentication
Although you could technically achieve everything with "single authentication", we highly recommend to apply 
dual authentication in the case you're building another service acting on behalf of an EBRAINS user:

Let's assume your service already authenticates the EBRAINS user (or has received the user token from an upstream service) 
and wants to read from the EBRAINS KG with the access rights of the user. In the dual authentication approach, 
you're sending the users token as a "Authorization" header (as in the single authentication) and additionally the 
client-token (which can be obtained as described in **Authentication with a client**) in a 
`Client-Authorization: Bearer YOUR_CLIENT_TOKEN` header.

### Why you should do this additional step

At a first glance, the dual-authentication might look like overhead. For a proper integration of your service into KG 
we would like to ask you to follow this pattern nevertheless to profit from the following benefits:

- Possibility to store client-specific information on top of the meta-data structures and to do contextualized queries
- Restrict user permissions to the ones of your clients (don't take the blame for erroneous data manipulation if your client is read-only)
- Possibility to relax "terms of use" consent requirements for end-users
- We can more easily support you
- We might be able to adapt the quality of service for your client
- Some features might only be available when being dual-authenticated

Long story short: You get more with minimal additional effort :)

## How to get your token 
There are different ways to get your token which depend on your use-case. The supported ways are dependent on the IAM system involved. 

The most simple case is if you already have a token which is either provided by the end-user or by the context 
(e.g. because you're using it in a the EBRAINS JupyterLab in which you already are logged in or because you have your 
own (web)application handling the authentication with EBRAINS IAM). If this is the case, you can just pass the token you've received in the
`Authorization: Bearer YOUR_TOKEN` header and you're good as long as the token includes the required scopes and is valid.

Unless the token is provided via simple user-input / configuration, possible oAuth flows to receive depend on your use cases:
- Single Page Applications (SPA) are typically recommended to use a "Authorization Code Flow with Proof Key for Code Exchange (PKCE)"
- Unsupervised scripts (without user interaction) or server2server scenarios without user authentication should use the "Client Credentials Flow" by using a service account (see *Authentication with a client*)
- Interactive, non-browser based clients (e.g. CLI) should use the "Device Authorization Flow"


Please note that the access token you're getting is short-lived.
Your script/code therefore needs to be prepared to refresh the access token (by requesting a new token with the client credentials again).
We usually achieve this by catching 401 status codes (Unauthorized) when executing a request on KG and by retrying the request again with a new token. 

Please note, that the official Python library for KG has convenience functionality to authorize with all of the above mentioned flows including an automated token refresh mechanism.

### Authentication with a client
Typical use-cases are unsupervised script or server 2 KG communications as well as the dual authentication case.

You should first create a "confidential" client as described in 
[Registering an OIDC client](https://wiki.ebrains.eu/bin/view/Collabs/the-collaboratory/Documentation%20IAM/FAQ/OIDC%20Clients%20explained/1.%20Registering%20an%20OIDC%20client%20v2/).   

Please note, that you can use an already existing client as long as it is "confidential". If your already existing client is "public", you need to register a second one. To ensure full compatibility with KG, make sure, you have the following settings to be configured:

- Access type
  - confidential 
- Client default scopes 
  - openid
  - email
  - group
  - profile
  - roles
  - team
- Service Accounts
  - enabled

Now that you have your client registered, you can easily fetch a token (without supervision) by using the client_credentials flow as described in [Use your OIDC client as service account](https://wiki.ebrains.eu/bin/view/Collabs/collaboratory-community-apps/Community%20App%20Developer%20Guide/Use%20your%20OIDC%20client%20as%20service%20account/)

