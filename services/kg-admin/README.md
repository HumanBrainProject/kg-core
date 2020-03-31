# Create a space

```plantuml
    
    "kg-admin" -> "kg-permissions": create space
    "kg-permissions" -> "kg-authentication": insert roles
    participant keycloak #Orange
    "kg-authentication" -> "keycloak": persist roles
    participant "nexus-v0-adapter" #Orange
    alt nexusv0 is configured
        "kg-authentication" -> "nexus-v0-adapter": persist roles
    end
     participant "nexus-v1-adapter" #Orange
    alt nexusv1 is configured
        "kg-authentication" -> "nexus-v1-adapter": persist roles 
    end
    "kg-admin" -> "kg-primarystore": post INSERT event for space
```

# Create a client
```plantuml
    "kg-admin" -> "kg-primarystore": post INSERT event for client
    "kg-admin" -> "kg-admin": create space for client
```


# Get user

```plantuml
    "kg-admin"->>"kg-auth": get user (token)
    "kg-auth"-->>"kg-admin": user info response
    "kg-admin"->>"kg-query": get user (id)
    "kg-query"-->>"kg-admin": user info response
    alt user does not exist
        "kg-admin"->>"kg-primary": if user does not exist -> postEvent (id, username)
        "kg-primary"-->>"kg-admin": done response
        "kg-admin"->>"kg-query": get user (id)
        "kg-query"-->>"kg-admin": user info response
    end
```