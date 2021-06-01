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
import eu.ebrains.kg.arango.commons.aqlBuilder.AQL;
import eu.ebrains.kg.arango.commons.aqlBuilder.ArangoVocabulary;
import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.arango.commons.model.ArangoDatabaseProxy;
import eu.ebrains.kg.authentication.model.AcceptedTermsOfUse;
import eu.ebrains.kg.authentication.model.ArangoTermsOfUse;
import eu.ebrains.kg.authentication.model.TermsOfUseAcceptance;
import eu.ebrains.kg.commons.JsonAdapter;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.model.TermsOfUse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

@Component
public class AuthenticationRepository {
    private final ArangoDatabaseProxy arangoDatabase;
    private final TermsOfUseRepository termsOfUseRepository;
    private final JsonAdapter jsonAdapter;

    @PostConstruct
    public void setup() {
        arangoDatabase.createIfItDoesntExist();
        arangoDatabase.createCollectionIfItDoesntExist("users");
        arangoDatabase.createCollectionIfItDoesntExist("publicSpaces");
    }

    public AuthenticationRepository(@Qualifier("termsOfUseDB") ArangoDatabaseProxy arangoDatabase, JsonAdapter jsonAdapter, TermsOfUseRepository termsOfUseRepository) {
        this.arangoDatabase = arangoDatabase;
        this.termsOfUseRepository = termsOfUseRepository;
        this.jsonAdapter = jsonAdapter;
    }

    private ArangoCollection getPublicSpacesCollection() {
        ArangoDatabase database = arangoDatabase.get();
        return database.collection("publicSpaces");
    }

    private ArangoCollection getUsersCollection() {
        ArangoDatabase database = arangoDatabase.get();
        return database.collection("users");
    }

    @Cacheable("termsOfUseByUser")
    public TermsOfUse findTermsOfUseToAccept(String userId){
        String userDoc = getUsersCollection().getDocument(userId, String.class);
        TermsOfUseAcceptance termsOfUse;
        if(userDoc!=null) {
            termsOfUse = jsonAdapter.fromJson(userDoc, TermsOfUseAcceptance.class);
        }
        else{
            termsOfUse = null;
        }
        TermsOfUse currentTermsOfUse = termsOfUseRepository.getCurrentTermsOfUse();
        if(currentTermsOfUse!=null) {
            String currentVersion = currentTermsOfUse.getVersion();
            if (termsOfUse != null && termsOfUse.getAcceptedTermsOfUse().stream().anyMatch(t -> t.getVersion().equals(currentVersion))) {
                return null;
            }
        }
        return currentTermsOfUse;
    }

    @CacheEvict(value="termsOfUseByUser", key = "#userId")
    public void acceptTermsOfUse(String version, String userId){
        TermsOfUseAcceptance userAcceptance = getUsersCollection().getDocument(userId, TermsOfUseAcceptance.class);
        if(userAcceptance == null){
            userAcceptance = new TermsOfUseAcceptance(userId, userId, new ArrayList<>());
        }
        userAcceptance.getAcceptedTermsOfUse().add(new AcceptedTermsOfUse(version, new Date()));
        getUsersCollection().insertDocument(jsonAdapter.toJson(userAcceptance), new DocumentCreateOptions().overwrite(true).silent(true));
    }

    public boolean isSpacePublic(String space){
        if(space!=null) {
            return getPublicSpacesCollection().documentExists(ArangoCollectionReference.fromSpace(new SpaceName(space)).getCollectionName());
        }
        return false;
    }

    @Cacheable("publicSpaces")
    public List<String> getPublicSpaces(){
        AQL aql = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        ArangoCollection publicSpacesCollection = getPublicSpacesCollection();
        aql.addLine(AQL.trust("FOR s IN @@spaceCollection FILTER s.`name` != NULL RETURN s.`name`"));
        bindVars.put("@spaceCollection", publicSpacesCollection.name());
        return arangoDatabase.getOrCreate().query(aql.build().getValue(), bindVars, String.class).asListRemaining();
    }


    @CacheEvict(value = "publicSpaces", allEntries = true)
    public void setSpacePublic(String space, boolean publicSpace) {
        String key = ArangoCollectionReference.fromSpace(new SpaceName(space)).getCollectionName();
        if(publicSpace){
            Map<String, Object> instance = new HashMap<>();
            instance.put(ArangoVocabulary.KEY, key);
            instance.put("name", space);
            getPublicSpacesCollection().insertDocument(instance, new DocumentCreateOptions().overwrite(true).silent(true));
        }
        else{
            getPublicSpacesCollection().deleteDocument(key);
        }
    }

}
