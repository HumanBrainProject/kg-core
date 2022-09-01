# Authorization
As described in the [authentication](authentication.html) section, the KG requires an authentication token of the requesting 
user for almost all provided API endpoints. 

Whilst the authentication is handled via a centralized identity management system (such as EBRAINS IAM),
the authorization definition is specified in the EBRAINS KG itself.

## Roles

The EBRAINS KG knows the following roles which can be applied to different area:

#### Consumer
A consumer is allowed to **read** the **released** resources of an instance.

#### Reviewer
A reviewer has all rights of a consumer and can in addition **read** instances from the **in progress** section

#### Editor
An editor has all rights of a reviewer and can in addition **write** and **delete** instances

#### Owner
An owner has all rights of an editor and can in addition **release** instances

#### Admin
An administrator has all possible functionalities at hand.

## Application of roles
All roles can be applied to one of the three granularity levels:

#### Global
This means that the given role applies globally for all spaces of the KG available

#### By space
The applied role is available for a specific space

#### By instance
The applied role is available for a specific instance

## Configuration of access permissions
The access permissions can be configured by the API endpoints for "setup/permissions". 
They allow to declare permissions by mapping a "claim" (as received from the user info 
endpoint of the authentication system) to an applied role.

### Example mappings

```json
  {
    "_key": "dataset:consumer",
    "authenticated": true
  }
```   
*All authenticated users are consumers of the space "dataset"*

```json
{
  "_key": "dataset:owner",
  "roles": {
    "group": [
      "group-dataset-curators"
    ]
  }
}
```
*All members of the group "group-dataset-curators" are owners of the space "dataset"*

```json
{
    "_key": "dataset:reviewer",
    "preferred_username": [
      "service-account-kg-search"
    ]
}
```   
*The user with the username "service-account-kg-search" is reviewer of the space "dataset"*

```json
{
    "_key": ":admin",
    "roles": {
      "group": [
        "group-kg-devs"
      ]
    }
}
```   
*All members of the group "group-kg-devs" are global administrators*

### Advanced examples
   

```json
{
    "_key": "$1:owner",
    "preferred_username": [
      "service-account-(.+)"
   ]
}
```
*All "service-account-" users are owner of the space of their client name*

```json
{
    "_key": "private-$1:owner",
    "sub": "(.+)"
}
```
*Every user is owner of the space with its own id (available in the "sub" claim)*

```json
[
  {
    "_key": "collab-$1:reviewer",
    "roles": {
      "team": [
        "collab-(.*)-viewer"
      ]
    }
  },
  {
    "_key": "collab-$1:owner",
    "roles": {
      "team": [
        "collab-(.*)-administrator"
      ]
    }
  },
  {
    "_key": "collab-$1:editor",
    "roles": {
      "team": [
        "collab-(.*)-editor"
      ]
    }
  }
]
```
*The collab-roles to space mapping: Members of a collab receive the corresponding roles in the KG space named by the collab*




