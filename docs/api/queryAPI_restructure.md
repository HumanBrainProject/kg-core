# Restructure and sort

Sometimes, it's useful to be able to sort instances by their values and/or to simplify their structures, especially if
they involve deep nestedness. This is what the query API allows you to do:

## Sorting the results alphabetically

Sorting the results alphabetically is e.g. interesting for pagination where you don't want to load all results and sort
them in a post-processing step (e.g. to populate a list of items to the end-user lazily loaded).
The specification of the `"sort": true` flag on one of the properties therefore allows to sort the instances by the
value of this property in an ascending order.

Please note, that you can specify "sort": true on **only one** property and only on the **root level**. If you need to
sort by a value originating from a nested value, you can use the *flattening* mechanism (see below) to map the nested
value to the root level.

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
    "path": "https://openminds.ebrains.eu/vocab/fullName",
    "sort": true
  }
}
```

**Response**

Expected: 200

```json
{
  "data": [
    {
      "https://openminds.ebrains.eu/vocab/fullName": "a"
    },
    {
      "https://openminds.ebrains.eu/vocab/fullName": "b"
    },
    ...
  ],
  "message": null,
  "error": null,
  "startTime": 1644940156713,
  "durationInMs": 83,
  "transactionId": null,
  "total": 1245,
  "size": 20,
  "from": 0
}
```

## Flattening structures

**Request**

```json
{
  "@context": {
    "@vocab": "https://core.kg.ebrains.eu/vocab/query/",
    "query": "https://schema.hbp.eu/myQuery/",
    "path": {
      "@type": "@id",
      "@id": "path"
    }
  },
  "meta": {
    "type": "https://openminds.ebrains.eu/core/DatasetVersion"
  },
  "structure": {
    "path": [
      "https://openminds.ebrains.eu/vocab/custodian",
      "https://openminds.ebrains.eu/vocab/familyName"
    ]
  }
}
```

**Response**

Expected: 200

```json
{
  "data": [
    {
      "https://schema.hbp.eu/myQuery/custodian": [
        "Foo",
        "Bar"
      ]
    },
    {
      "https://schema.hbp.eu/myQuery/custodian": [
        "Hello",
        "World"
      ]
    },
    ...
  ],
  "message": null,
  "error": null,
  "startTime": 1645025828640,
  "durationInMs": 33,
  "transactionId": null,
  "total": 143,
  "size": 20,
  "from": 0
}
```

## Single item (to be implemented)
If you want to ensure that you only retrieve a single value for a specific property, you can set "singleValue" to true.

> **Attention**   
> The Query API will return the first item only and will not cross-check if there are any additional elements
> which could potentially be ignored. 


[//TODO]: <> (Implement according to documentation)

**Request**

```json
{
  "@context": {
    "@vocab": "https://core.kg.ebrains.eu/vocab/query/",
    "query": "https://schema.hbp.eu/myQuery/",
    "propertyName": {
      "@type": "@id",
      "@id": "propertyName"
    },
    "path": {
      "@type": "@id",
      "@id": "path"
    }
  },
  "meta": {
    "type": "https://openminds.ebrains.eu/core/DatasetVersion"
  },
  "structure": {
    "propertyName": "query:custodian",
    "path": [
      "https://openminds.ebrains.eu/vocab/custodian",
      "https://openminds.ebrains.eu/vocab/familyName"
    ],
    "singleValue": true
  }
}
```

**Response**

Expected: 200

```json 
{
    "data": [
        {
            "https://schema.hbp.eu/myQuery/custodian": "Foo"
        },
        {
            "https://schema.hbp.eu/myQuery/custodian": "Hello"
        },
        ...
    ],
    "message": null,
    "error": null,
    "startTime": 1645025828640,
    "durationInMs": 33,
    "transactionId": null,
    "total": 143,
    "size": 20,
    "from": 0
}
```