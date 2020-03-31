/*
 * Copyright 2020 EPFL/Human Brain Project PCO
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.ebrains.kg.graphdb.types.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class StructureController {

    @Autowired
    ArangoRepositoryTypes arangoRepository;

//    public List<StructureController> getAllTypes(){
//        ret
//    }
//
//    public List<StructureController> getTypesBySpace(Space space){
//
//    }


//    public List<StructureOfType> getStructureOfTypesInSpace(DataStage dataStage, Space space, Client client, boolean withFields) {
//        return getStructureOfTypes(dataStage, space, null, client, withFields);
//    }
//
//    public List<StructureOfType> getTypes(DataStage dataStage,  List<String> types, Client client, boolean withFields){
//        return getStructureOfTypes(dataStage, types, client, withFields);
//    }
//
//    private List<StructureOfType> getStructureOfTypes(DataStage dataStage, List<String> types, Client client, boolean withFields) {
//        List<StructureOfType> structureOfTypes = arangoRepository.getTypes(dataStage, types, client);
//        if (withFields) {
//            List<StructureOfType> updatedStructureOfType = structureOfTypes.stream().map(t -> {
//                List<StructureOfField> structureOfFields = fetchFieldsFromType(dataStage, t.getType(), client);
//                return new StructureOfType(t.getType(), t.getLabel(), structureOfFields, t.getClientStructureOfType());
//            }).collect(Collectors.toList());
//            return updatedStructureOfType;
//        } else {
//            return structureOfTypes;
//        }
//    }
//
//    public StructureOfType getStructureOfType(DataStage dataStage, Space space, String type, Client client, boolean withFields) {
//        List<StructureOfType> structureOfTypes = arangoRepository.getTypes(dataStage, space, Collections.singletonList(type), client);
//        if(structureOfTypes.size() > 0){
//            StructureOfType structureOfType = structureOfTypes.get(0);
//            if(withFields){
//                List<StructureOfField> structureOfFields = fetchFieldsFromType(dataStage, space, structureOfType.getType(), client);
//                return new StructureOfType(structureOfType.getType(), structureOfType.getLabel(), structureOfFields, structureOfType.getClientStructureOfType());
//            }else {
//                return structureOfType;
//            }
//        }
//        return null;
//    }
//
//    public void addTypeFieldInfo(DataStage dataStage, Space space, String type, String fieldName, StructureOfField payload){
//        arangoRepository.addTypeFieldInfo(dataStage, space, type, fieldName, payload);
//    }
//
//    public List<StructureOfField> fetchFieldsFromType(DataStage dataStage,Space space, String type, Client client){
//        return arangoRepository.getTypeFields(dataStage,space, type, client);
//    }

}
