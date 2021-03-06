/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
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

package eu.ebrains.kg.core.api.types.test;

import com.arangodb.ArangoDB;
import eu.ebrains.kg.authentication.api.AuthenticationAPI;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.permission.roles.RoleMapping;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.core.api.AbstractTest;
import eu.ebrains.kg.core.api.Types;

public class DefineTypeTest extends AbstractTest {

    private final Types types;
    public final NormalizedJsonLd typeDefinition = createTypeDefinition();

    public DefineTypeTest(ArangoDB.Builder database, AuthenticationAPI authenticationAPI,  RoleMapping[] roles, Types types) {
        super(database, authenticationAPI,  roles);
        this.types = types;
    }

    public final String typeName = "https://core.kg.ebrains.eu/Test";

    NormalizedJsonLd createTypeDefinition() {
        NormalizedJsonLd payload = new NormalizedJsonLd();
        payload.addProperty(EBRAINSVocabulary.META_TYPE, new JsonLdId(typeName));
        payload.addProperty("http://foo", "bar");
        return payload;
    }

    @Override
    protected void setup() {

    }

    @Override
    protected void run() {
        types.defineType(typeDefinition, true);
    }
}
