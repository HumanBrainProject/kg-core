# Basics

These are some basic queries to get started with the syntax of the query api.

## Minimal payload

**Request**

```json
{
  "@context": {
    "@vocab": "https://core.kg.ebrains.eu/vocab/query/",
    "propertyName": {
      "@id": "propertyName",
      "@type": "@id"
    }
  },
  "meta": {
    "type": "https://openminds.ebrains.eu/core/DatasetVersion"
  },
  "structure": [
    {
      "path": "@id"
    }
  ]
}
```

**Response**

Expected: 200

```json
{
  "data": [
    {
      "@id": "foo bar"
    },
    ...
  ],
  "message": null,
  "error": null,
  "startTime": 1644935130702,
  "durationInMs": 36,
  "transactionId": null,
  "total": 1245,
  "size": 20,
  "from": 0
}
```

## Reducing arrays to objects for single values

When only a single field (at a specific level in the tree) is requested Structure can be defined as an object instead of
an array of objects

**Request**

```json
{
  "@context": {
    "@vocab": "https://core.kg.ebrains.eu/vocab/query/",
    "propertyName": {
      "@id": "propertyName",
      "@type": "@id"
    }
  },
  "meta": {
    "type": "https://openminds.ebrains.eu/core/DatasetVersion"
  },
  "structure": {
    "path": "@id"
  }
}
```

## Specifying a non existing field

**Request**

```json
{
  "@context": {
    "@vocab": "https://core.kg.ebrains.eu/vocab/query/",
    "propertyName": {
      "@id": "propertyName",
      "@type": "@id"
    }
  },
  "meta": {
    "type": "https://openminds.ebrains.eu/core/DatasetVersion"
  },
  "structure": {
    "path": "non_existing_field"
  }
}
```

**Response**

Expected: 200

```json
{
  "data": [
    {
      "non_existing_field": null
    },
    ...
  ],
  "message": null,
  "error": null,
  "startTime": 1644935130702,
  "durationInMs": 36,
  "transactionId": null,
  "total": 1245,
  "size": 20,
  "from": 0
}
```

## Renaming a property to a custom name

**Request**

```json
{
  "@context": {
    "@vocab": "https://core.kg.ebrains.eu/vocab/query/",
    "propertyName": {
      "@id": "propertyName",
      "@type": "@id"
    }
  },
  "meta": {
    "type": "https://openminds.ebrains.eu/core/DatasetVersion"
  },
  "structure": [
    {
      "propertyName": "foo",
      "path": "@id"
    },
    {
      "propertyName": "bar",
      "path": "https://openminds.ebrains.eu/vocab/fullName"
    }
  ]
}
```

**Response**

Expected: 200

```json
{
  "data": [
    {
      "foo": "foo value",
      "bar": "bar value"
    },
    ...
  ],
  "message": null,
  "error": null,
  "startTime": 1644937956881,
  "durationInMs": 29,
  "transactionId": null,
  "total": 1245,
  "size": 20,
  "from": 0
}
```

## Semantic properties

You should aim for semantically correct defined properties - even if you're renaming them. To do so, you can specify the
property name in a [compact IRI](https://www.w3.org/TR/json-ld11/#dfn-compact-iri) format

**Request**

```json
{
  "@context": {
    "@vocab": "https://core.kg.ebrains.eu/vocab/query/",
    "query": "https://schema.foo.bar/myQuery/",
    "propertyName": {
      "@id": "propertyName",
      "@type": "@id"
    }
  },
  "meta": {
    "type": "https://openminds.ebrains.eu/core/DatasetVersion"
  },
  "structure": [
    {
      "propertyName": "query:foo",
      "path": "@id"
    },
    {
      "propertyName": "query:bar",
      "path": "https://openminds.ebrains.eu/vocab/fullName"
    }
  ]
}
```

**Response**

Expected: 200

```json
{
  "data": [
    {
      "https://schema.foo.bar/myQuery/foo": "foo value",
      "https://schema.foo.bar/myQuery/bar": "bar value"
    },
    ...
  ],
  "message": null,
  "error": null,
  "startTime": 1644937956881,
  "durationInMs": 29,
  "transactionId": null,
  "total": 1245,
  "size": 20,
  "from": 0
}
```

*A query with a semantically correct propertyName (either compact or expanded IRI) will lead to a full IRI
representation in the response*

## Simplify a response with "responseVocab"

By adding a "responseVocab" to the meta section of the query, the extra complexity of the full IRI representation (see
above) can be removed

**Request**

```json
{
  "@context": {
    "@vocab": "https://core.kg.ebrains.eu/vocab/query/",
    "query": "https://schema.foo.bar/myQuery/",
    "propertyName": {
      "@id": "propertyName",
      "@type": "@id"
    }
  },
  "meta": {
    "type": "https://openminds.ebrains.eu/core/DatasetVersion",
    "responseVocab": "https://schema.foo.bar/myQuery/"
  },
  "structure": [
    {
      "propertyName": "query:foo",
      "path": "@id"
    },
    {
      "propertyName": "query:bar",
      "path": "https://openminds.ebrains.eu/vocab/fullName"
    }
  ]
}
```

**Response**

Expected: 200

```json
{
  "data": [
    {
      "@context": {
        "@vocab": "https://schema.foo.bar/myQuery/"
      },
      "bar": "bar value",
      "foo": "foo value"
    },
    ...
  ],
  "message": null,
  "error": null,
  "startTime": 1644938488274,
  "durationInMs": 29,
  "transactionId": null,
  "total": 1245,
  "size": 20,
  "from": 0
}
```

*A simplified response structure by the application of a responseContext. Please note the "@context" inside of the
response object which makes it again a fully valid JSON-LD whilst still allowing to process it in a simple way with any
JSON processor*

## Ensuring order of properties

If you are not familiar with linked data in general, this might feel odd to you: If an instance links to multiple other
objects
(e.g. a Dataset pointing to its authors), in a graph database, the way they are ordered is not ensured by default. This
is because
the array of links becomes actual graph edges which do not keep the state between themselves. In many cases, this is not
a problem since
most of the time, the order within the specified links doesn't matter. Sometimes it is crucial though since the order
carries implicit meaning.
A perfect example for this is the mentioned "author" list where it is important if a contributor is first or last
author.

The query API by default therefore doesn't maintain the order of insertion when returning items. However, it allows you
to explicitly request the order
by specifying the "ensureOrder" property to true:

**Request**

```json
{
  "@context": {
    "@vocab": "https://core.kg.ebrains.eu/vocab/query/",
    "path": {
      "@id": "path",
      "@type": "@id"
    }
  },
  "meta": {
    "type": "https://openminds.ebrains.eu/core/DatasetVersion"
  },
  "structure": {
    "path": "https://openminds.ebrains.eu/vocab/custodian",
    "ensureOrder": true,
    "structure": {
      "path": "@id"
    }
  }
}
```

**Response**

Expected: 200

```json 
{
    "data": [
        {
            "https://openminds.ebrains.eu/vocab/custodian": [
                {
                    "@id": "https://kg.ebrains.eu/api/instances/a7745fd1-f4b7-4118-a88b-7f57758c836e"
                },
            ...
            ]
        },
        ...
    ],
    "message": null,
    "error": null,
    "startTime": 1645024403178,
    "durationInMs": 29,
    "transactionId": null,
    "total": 144,
    "size": 20,
    "from": 0
}
```
