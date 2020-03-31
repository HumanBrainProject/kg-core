#!/bin/bash

pids=""
build() {
  pids=""
  mvn -DskipTests=true -f $1/$2 clean javadoc:javadoc install&
  pids+=" $!"
}

waiting() {
  for p in $pids; do
        if wait $p; then
              echo "Process $p success"
        else
              echo "Process $p fail"
              exit 1
        fi
  done
}


build config kg-library
waiting
build libs kg-commons
waiting
build libs-4-test kg-test-utils
waiting
build libs kg-microservice-commons
waiting
build libs kg-client-commons
waiting
build libs-4-test kg-spring-test-utils
waiting
build libs kg-arango-commons
waiting
build config kg-microservice
waiting

build services kg-service-discovery
waiting

build services kg-admin
build services kg-authentication
build services kg-core-api
build services kg-graphdb-commons
build services kg-ids
waiting
build services kg-graphdb-sync
#build services kg-graphdb-async
build services kg-inference
build services kg-indexing
waiting
build services kg-jsonld
build services kg-primarystore
build services kg-query
build services kg-releasing
waiting
build services kg-spatial-search
build adapters kg-nexus-v0-adapter
#build adapters kg-nexus-v1-adapter
build tests kg-system-test
#build tests kg-core-to-query-comparison
#build mocks kg-core-api-mock
waiting


