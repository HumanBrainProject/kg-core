#!/bin/bash
REGISTRY=$1
LABEL=$2
STAGE=$3
tag() {
  docker pull $REGISTRY/$1:$LABEL
  docker tag $REGISTRY/$1:$LABEL $REGISTRY/$1:$STAGE
  docker push $REGISTRY/$1:$STAGE
}
tag kg-admin
tag kg-ids
tag kg-authentication
tag kg-core-api
tag kg-graphdb-sync
#tag kg-graphdb-async
tag kg-inference
tag kg-indexing
tag kg-jsonld-node
tag kg-jsonld
tag kg-primarystore
tag kg-query
tag kg-releasing
tag kg-service-discovery
tag kg-spatial-search
tag kg-nexus-v0-adapter
#tag kg-nexus-v1-adapter

#tag kg-core-api-mock


