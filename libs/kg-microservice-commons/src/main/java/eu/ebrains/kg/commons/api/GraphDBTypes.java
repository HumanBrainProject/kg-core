/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 * Copyright 2021 - 2022 EBRAINS AISBL
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This open source software code was developed in part or in whole in the
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 */

package eu.ebrains.kg.commons.api;

import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.model.external.types.TypeInformation;

import java.util.List;
import java.util.Map;

public interface GraphDBTypes {

    interface Client extends GraphDBTypes {}

    Paginated<TypeInformation> listTypes(DataStage stage, String space, boolean withProperties, boolean withIncomingLinks, PaginationParam paginationParam);

    Map<String, Result<TypeInformation>> getTypesByName(List<String> types, DataStage stage, String space, boolean withProperties, boolean withIncomingLinks);

    void specifyType(JsonLdId typeName, NormalizedJsonLd normalizedJsonLd, boolean global);

    void removeTypeSpecification(JsonLdId typeName, boolean global);

    void specifyProperty(JsonLdId propertyName, NormalizedJsonLd normalizedJsonLd, boolean global);

    void removePropertySpecification(JsonLdId propertyName, boolean global);

    void addOrUpdatePropertyToType(String typeName, String propertyName, NormalizedJsonLd payload, boolean global);

    void removePropertyFromType(String typeName, String propertyName, boolean global);

}
