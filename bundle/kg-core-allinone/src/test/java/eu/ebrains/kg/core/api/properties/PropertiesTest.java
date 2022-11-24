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

package eu.ebrains.kg.core.api.properties;

import eu.ebrains.kg.commons.exception.ForbiddenException;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.commons.model.external.types.TypeInformation;
import eu.ebrains.kg.commons.permission.roles.RoleMapping;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;
import eu.ebrains.kg.core.api.v3.InstancesV3;
import eu.ebrains.kg.core.api.v3.PropertiesV3;
import eu.ebrains.kg.core.api.v3.TypesV3;
import eu.ebrains.kg.core.api.properties.test.DefinePropertyGlobalTest;
import eu.ebrains.kg.core.model.ExposedStage;
import eu.ebrains.kg.testutils.AbstractFunctionalityTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PropertiesTest extends AbstractFunctionalityTest {

    @Autowired
    TypesV3 types;

    @Autowired
    PropertiesV3 properties;

    @Autowired
    InstancesV3 instances;

    private final static RoleMapping[] PROPERTY_DEFINITION_ROLES = new RoleMapping[]{RoleMapping.ADMIN};
    private final static RoleMapping[] NON_PROPERTY_DEFINITION_ROLES = RoleMapping.getRemainingUserRoles(PROPERTY_DEFINITION_ROLES);


    @Test
    void definePropertyGlobalOk() {
        //Given
        DefinePropertyGlobalTest test = new DefinePropertyGlobalTest(ctx(PROPERTY_DEFINITION_ROLES), instances, properties);

        //When
        test.execute(() -> {
            //Then
            Map<String, Result<TypeInformation>> result = test.assureValidPayload(this.types.getTypesByName(Collections.singletonList(test.type), ExposedStage.IN_PROGRESS, true, true, null));
            TypeInformation typeDefinition = test.assureValidPayload(result.get(test.type));
            List<NormalizedJsonLd> properties = typeDefinition.getAsListOf(EBRAINSVocabulary.META_PROPERTIES, NormalizedJsonLd.class);
            NormalizedJsonLd propertydef = properties.stream().filter(p -> p.getAs(SchemaOrgVocabulary.IDENTIFIER, String.class).equals(test.property)).findFirst().orElse(null);
            assertEquals("bar", propertydef.getAs("http://foo", String.class));
        });
    }


    @Test
    @SuppressWarnings("java:S2699") //The assertion is handled within the "execution" part.
    void definePropertyGlobalForbidden() {
        //Given
        DefinePropertyGlobalTest test = new DefinePropertyGlobalTest(ctx(NON_PROPERTY_DEFINITION_ROLES), instances, properties);

        //When
        test.execute(ForbiddenException.class);
    }

}
