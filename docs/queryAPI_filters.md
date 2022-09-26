# Filters

Filters are key to reduce the results to only those items you are actually interested in.

There are two types of filter:

- Instance filters
- Property filters

**Instance filters** are meant to reduce the instances either at the root level (resulting in less instances returned)
or in graph traversals to e.g. only take into account instances of a specific type (**type filters**) or those that
contain a specific value (**required** filter)

**Property filters** are applied on the values of the properties themselves. Typical use cases are filters checking if a
value is similar to a given value or not. We have multiple comparison operators available:

- EQUALS: The value has to be the same as it is in the instance. For the instance value "hello world" only the filter
  value "hello world" would match
- STARTS_WITH: The value has to start with the given sequence. For the instance value "hello world", this would be the
  case for the filter value "hello"
- ENDS_WITH: The value has to end with the given sequence. For the instance value "hello world", this would be the case
  for "world"
- CONTAINS: The value is returned if it contains the given filter value at any place. This is e.g. true when filtering
  for "wor" for the instance value "hello world"
- REGEX: You can pass any regular expression. For the instance value "hello world", a filter value like ".*lo\sworl.*"
  would hit.

Please note, that those filters differ in terms of computational complexity. Although you could achieve all of the above
solely with the REGEX filter, it's highly recommended to use them according to the above order top profit from the best
performance (EQUALS is less expensive than STARTS_WITH is less expensive than CONTAINS ...).

Also, it's important to understand, that if a filter is set to a property, this automatically means that this property
is
"required" for the instance and therefore instances that do not match the filter are going to be ignored.


> **Attention**   
> Some properties can contain multiple values / arrays (either natively by data structure or because they are *
> flattened*).
> Please be aware that property filters will be evaluated to true if one of the multiple values fulfills the criteria.
> If this is the case, all values of the property are returned and **NOT** only the ones matching the filter.
> This is on purpose since a reduction of values inside a property could lead to serious misconceptions at
> interpretation time.


**Special case: IS_EMPTY**   
There is one special case for filters which is IS_EMPTY. This is for those cases, when you explicitly want to ask for
a property not to exist.

## Type filter

The type filter allows to restrict the possible results of a graph traversals to those instances being of a specific
type:

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
    "path": {
      "@id": "https://openminds.ebrains.eu/vocab/custodian",
      "typeFilter": {
        "@id": "https://openminds.ebrains.eu/core/Organization"
      }
    },
    "required": true,
    "structure": {
      "path": "https://openminds.ebrains.eu/vocab/familyName"
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
          "https://openminds.ebrains.eu/vocab/fullName": "EPFL"
        },
        ...
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

### Type filter with multiple types

The type filter can also be applied to multiple types:

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
    "path": {
      "@id": "https://openminds.ebrains.eu/vocab/custodian",
      "typeFilter": [
        {
          "@id": "https://openminds.ebrains.eu/core/Organization"
        },
        {
          "@id": "https://openminds.ebrains.eu/core/Person"
        }
      ]
    },
    "structure": [
      {
        "path": "https://openminds.ebrains.eu/vocab/fullName"
      },
      {
        "path": "https://openminds.ebrains.eu/vocab/familyName"
      }
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
      "https://openminds.ebrains.eu/vocab/custodian": [
        {
          "https://openminds.ebrains.eu/vocab/fullName": "EPFL",
          "https://openminds.ebrains.eu/vocab/familyName": null
        },
        {
          "https://openminds.ebrains.eu/vocab/fullName": null,
          "https://openminds.ebrains.eu/vocab/familyName": "Foo"
        },
        ...
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

## "Required" filter

Often, you might want to filter out all instances which do not contain a specific value. This is what the "required"
filter is for.
It means that if a field is not specified or has an empty value (e.g. an empty array), it's regarded to not contain the
value. If this is
the case, the instance is not part of the listed entities.

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
    "path": "non_existing_field",
    "required": true
  }
}
```

**Response**

Expected: 200

```json 
{
  "data": [],
  "message": null,
  "error": null,
  "startTime": 1644936346646,
  "durationInMs": 28,
  "transactionId": null,
  "total": 0,
  "size": 0,
  "from": 0
}
```

## Equals

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
    "structure": {
      "path": "https://openminds.ebrains.eu/vocab/givenName",
      "filter": {
        "op": "EQUALS",
        "value": "giacomo"
      }
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
          "https://openminds.ebrains.eu/vocab/givenName": "Giacomo"
        }
      ]
    },
    ...
  ],
  "message": null,
  "error": null,
  "startTime": 1645108914079,
  "durationInMs": 92,
  "transactionId": null,
  "total": 5,
  "size": 20,
  "from": 0
}
```

## Starts with

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
    "structure": {
      "path": "https://openminds.ebrains.eu/vocab/givenName",
      "filter": {
        "op": "STARTS_WITH",
        "value": "gia"
      }
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
          "https://openminds.ebrains.eu/vocab/givenName": "Giacomo"
        }
      ]
    },
    ...
  ],
  "message": null,
  "error": null,
  "startTime": 1645108914079,
  "durationInMs": 92,
  "transactionId": null,
  "total": 5,
  "size": 20,
  "from": 0
}
```

## Ends with

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
    "structure": {
      "path": "https://openminds.ebrains.eu/vocab/givenName",
      "filter": {
        "op": "ENDS_WITH",
        "value": "como"
      }
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
          "https://openminds.ebrains.eu/vocab/givenName": "Giacomo"
        }
      ]
    },
    ...
  ],
  "message": null,
  "error": null,
  "startTime": 1645108914079,
  "durationInMs": 92,
  "transactionId": null,
  "total": 5,
  "size": 20,
  "from": 0
}
```


## Contains

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
    "structure": {
      "path": "https://openminds.ebrains.eu/vocab/givenName",
      "filter": {
        "op": "CONTAINS",
        "value": "aco"
      }
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
          "https://openminds.ebrains.eu/vocab/givenName": "Giacomo"
        }
      ]
    },
  ...
  ],
  "message": null,
  "error": null,
  "startTime": 1645108914079,
  "durationInMs": 92,
  "transactionId": null,
  "total": 5,
  "size": 20,
  "from": 0
}
```

## Regex

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
    "structure": {
      "path": "https://openminds.ebrains.eu/vocab/givenName",
      "filter": {
        "op": "REGEX",
        "value": "^.*aco.*$"
      }
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
          "https://openminds.ebrains.eu/vocab/givenName": "Giacomo"
        }
      ]
    },
    ...
  ],
  "message": null,
  "error": null,
  "startTime": 1645108914079,
  "durationInMs": 92,
  "transactionId": null,
  "total": 5,
  "size": 20,
  "from": 0
}
```


## Empty value

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
    "structure": [
      {
        "path": "@id"
      },
      {
        "path": "https://openminds.ebrains.eu/vocab/fullName",
        "filter": {
          "op": "IS_EMPTY"
        }
      }
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
      "https://openminds.ebrains.eu/vocab/custodian": [
        {
          "@id": "https://kg.ebrains.eu/api/instances/a7745fd1-f4b7-4118-a88b-7f57758c836e",
          "https://openminds.ebrains.eu/vocab/fullName": null
        },
        ...
      ]
    },
    ...
  ],
  "message": null,
  "error": null,
  "startTime": 1645095615676,
  "durationInMs": 65,
  "transactionId": null,
  "total": 142,
  "size": 20,
  "from": 0
}
```


### Filter empty array

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
    "filter": {
      "op": "IS_EMPTY"
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
      "https://openminds.ebrains.eu/vocab/custodian": []
    },
    ...
  ],
  "message": null,
  "error": null,
  "startTime": 1645108203595,
  "durationInMs": 51,
  "transactionId": null,
  "total": 204,
  "size": 20,
  "from": 0
}
```
