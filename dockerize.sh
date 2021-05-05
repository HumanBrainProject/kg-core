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


