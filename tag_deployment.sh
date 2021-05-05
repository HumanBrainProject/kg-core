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


