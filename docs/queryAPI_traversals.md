# Traversals

One of the most interesting applications of the query API is to easily traverse the graph and collect data from multiple
linked entities.

## Walk the graph

By specifying substructures, a query can walk down the graph as many levels as wished. Of course, the depth of the query has an impact on performance. 

> **Advice**   
> To improve the performance of your query, try to write it in a way you filter out as many instances as "early" as possible. This means, that (in general) it's preferable to have filters as close to the first level of nesting.

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
  "structure": [
    {
      "path": "https://openminds.ebrains.eu/vocab/fullName"
    },
    {
      "path": "https://openminds.ebrains.eu/vocab/studiedSpecimen",
      "structure": [
        {
          "path": "https://openminds.ebrains.eu/vocab/lookupLabel"
        },
        {
          "path": "https://openminds.ebrains.eu/vocab/species",
          "structure": {
            "path": "https://openminds.ebrains.eu/vocab/name"
          }
        }
      ]
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
      "https://openminds.ebrains.eu/vocab/fullName": "3D reconstruction and analysis of brain cortex samples",
      "https://openminds.ebrains.eu/vocab/studiedSpecimen": [
        {
          "https://openminds.ebrains.eu/vocab/lookupLabel": "SG_healthy_adult",
          "https://openminds.ebrains.eu/vocab/species": [
            {
              "https://openminds.ebrains.eu/vocab/name": "Homo sapiens"
            },
            ...
          ]
        },
        ...
      ]
    },
    ...
  ],
  "message": null,
  "error": null,
  "startTime": 1645009363107,
  "durationInMs": 29,
  "transactionId": null,
  "total": 1245,
  "size": 20,
  "from": 0
}
```

## Reverse traversals

Sometimes, it's not the root instance pointing to other linked elements but the other way around. This is what we call "reverse links". 
It's easy to tell the KG Query API in which direction you want to walk the graph by specifying the "reverse" property (which defaults to `false` if not specified).

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
    "type": "https://openminds.ebrains.eu/core/TissueSample"
  },
  "structure": [
    {
      "path": "https://openminds.ebrains.eu/vocab/lookupLabel"
    },
    {
      "path": {
        "@id": "https://openminds.ebrains.eu/vocab/studiedSpecimen",
        "reverse": true
      },
      "structure": {
        "path": "https://openminds.ebrains.eu/vocab/fullName"
      }
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
      "https://openminds.ebrains.eu/vocab/lookupLabel": "XXX00-XX0",
      "https://openminds.ebrains.eu/vocab/studiedSpecimen": [
        {
          "https://openminds.ebrains.eu/vocab/fullName": "Synapse maps"
        }
      ]
    },
    ...
  ],
  "message": null,
  "error": null,
  "startTime": 1645010346904,
  "durationInMs": 39,
  "transactionId": null,
  "total": 303,
  "size": 20,
  "from": 0
}
```