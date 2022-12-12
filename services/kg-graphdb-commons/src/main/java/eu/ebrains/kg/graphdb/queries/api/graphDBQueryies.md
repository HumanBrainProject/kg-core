# GraphDB queries API

```mermaid
sequenceDiagram   
  autonumber
  Calling service ->> Graph DB: execute query
  Graph DB ->> Graph DB: Validate token
  Graph DB ->> Specification interpreter: parse query
  Specification interpreter -->> Graph DB: specification
  alt scope mode:
      Graph DB ->> Graph DB: translate
  else:
      Graph DB ->> Graph DB: define instance filters for user
  end
  Graph DB ->> Data query builder: Generate AQL
  Data query builder -->> Graph DB: AQL
  Graph DB ->> Graph DB: Apply id restrictions
  Graph DB ->> Arango: Run query
  Arango -->> Graph DB: paginated stream
  Graph DB -->> Calling service: streamed query result
```

3: parse, normalize and generate a model for the query specification   
5: translate the query to only return scope information only (ids of involved instances). Please note, that we're explicitly not setting the filters in this mode since no data (other than ids) is exposed.
