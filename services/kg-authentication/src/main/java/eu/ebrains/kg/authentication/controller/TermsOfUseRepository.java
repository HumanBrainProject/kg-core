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

package eu.ebrains.kg.authentication.controller;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.model.DocumentCreateOptions;
import eu.ebrains.kg.arango.commons.model.ArangoDatabaseProxy;
import eu.ebrains.kg.authentication.model.ArangoTermsOfUse;
import eu.ebrains.kg.commons.JsonAdapter;
import eu.ebrains.kg.commons.model.TermsOfUse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;

@Component
public class TermsOfUseRepository {
    private final ArangoDatabaseProxy arangoDatabase;
    private final JsonAdapter jsonAdapter;


    @PostConstruct
    public void setup() {
        arangoDatabase.createIfItDoesntExist();
        arangoDatabase.createCollectionIfItDoesntExist("termsOfUse");
    }

    public TermsOfUseRepository(@Qualifier("termsOfUseDB") ArangoDatabaseProxy arangoDatabase, JsonAdapter jsonAdapter) {
        this.arangoDatabase = arangoDatabase;
        this.jsonAdapter = jsonAdapter;
    }


    private ArangoCollection getTermsOfUseCollection() {
        ArangoDatabase database = arangoDatabase.getOrCreate();
        return database.collection("termsOfUse");
    }

    @Cacheable("termsOfUse")
    public TermsOfUse getCurrentTermsOfUse() {
        return getTermsOfUseCollection().getDocument("current", TermsOfUse.class);
    }

    @CacheEvict(value = "termsOfUse", allEntries = true)
    public void setCurrentTermsOfUse(TermsOfUse termsOfUse) {
        if(termsOfUse==null || termsOfUse.getData() == null || termsOfUse.getVersion() == null){
            throw new IllegalArgumentException("Was receiving an invalid terms of use specification");
        }
        ArangoTermsOfUse versioned = new ArangoTermsOfUse(termsOfUse.getVersion(), termsOfUse.getData(), termsOfUse.getVersion());
        ArangoTermsOfUse current = new ArangoTermsOfUse(termsOfUse.getVersion(), termsOfUse.getData(), "current");
        getTermsOfUseCollection().insertDocuments(Arrays.asList(jsonAdapter.toJson(versioned), jsonAdapter.toJson(current)), new DocumentCreateOptions().overwrite(true).silent(true));
    }


}
