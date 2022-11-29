# JSON-LD 

## Normalize payload

```mermaid
sequenceDiagram   
  actor Client
  Client ->> Core API: normalize payload
  Core API ->> JSON-LD: normalize payload
  JSON-LD -->> Core API: normalized payload
  Core API -->> Client: normalized payload
```