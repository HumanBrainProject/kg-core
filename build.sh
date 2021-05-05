#!/bin/bash

#
# Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0.
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# This open source software code was developed in part or in whole in the
# Human Brain Project, funded from the European Union's Horizon 2020
# Framework Programme for Research and Innovation under
# Specific Grant Agreements No. 720270, No. 785907, and No. 945539
# (Human Brain Project SGA1, SGA2 and SGA3).
#

pids=""
build() {
  pids=""
  mvn -DskipTests=true -f $1/$2 clean install&
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


