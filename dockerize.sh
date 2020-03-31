#!/bin/bash
REGISTRY=$1
DO_PUSH=$2
dockerize() {
  cd $1/$2 && docker build -t $REGISTRY/$2:latest --build-arg JAR_FILE=target/$2.jar . && cd ../..
  case $DO_PUSH in
    (true)  docker push $REGISTRY/$2:latest;;
  esac
}
dockerize services kg-admin
dockerize services kg-ids
dockerize services kg-authentication
dockerize services kg-core-api
dockerize services kg-graphdb-sync
#dockerize services kg-graphdb-async
dockerize services kg-inference
dockerize services kg-indexing
dockerize services kg-jsonld-node
dockerize services kg-jsonld
dockerize services kg-primarystore
dockerize services kg-query
dockerize services kg-releasing
dockerize services kg-service-discovery
dockerize services kg-spatial-search
dockerize adapters kg-nexus-v0-adapter
#dockerize adapters kg-nexus-v1-adapter
dockerize tests kg-system-test
#dockerize tests kg-core-to-query-comparison
#dockerize mocks kg-core-api-mock


