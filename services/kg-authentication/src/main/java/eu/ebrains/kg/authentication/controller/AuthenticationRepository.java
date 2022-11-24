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

package eu.ebrains.kg.authentication.controller;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.model.DocumentCreateOptions;
import eu.ebrains.kg.arango.commons.aqlbuilder.AQL;
import eu.ebrains.kg.arango.commons.aqlbuilder.ArangoVocabulary;
import eu.ebrains.kg.arango.commons.model.ArangoDatabaseProxy;
import eu.ebrains.kg.authentication.model.AcceptedTermsOfUse;
import eu.ebrains.kg.authentication.model.InstanceScope;
import eu.ebrains.kg.authentication.model.Invitation;
import eu.ebrains.kg.authentication.model.TermsOfUseAcceptance;
import eu.ebrains.kg.commons.JsonAdapter;
import eu.ebrains.kg.commons.SetupLogic;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.model.TermsOfUse;
import eu.ebrains.kg.commons.permission.roles.Role;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class AuthenticationRepository implements SetupLogic {

    private final ArangoDatabaseProxy arangoDatabase;
    private final TermsOfUseRepository termsOfUseRepository;
    private final JsonAdapter jsonAdapter;

    @PostConstruct
    public void setup() {
        arangoDatabase.createIfItDoesntExist();
        arangoDatabase.createCollectionIfItDoesntExist("users");
        arangoDatabase.createCollectionIfItDoesntExist("permissions");
        arangoDatabase.createCollectionIfItDoesntExist("invitations");
        arangoDatabase.createCollectionIfItDoesntExist("instanceScopes");
    }

    public AuthenticationRepository(@Qualifier("termsOfUseDB") ArangoDatabaseProxy arangoDatabase, JsonAdapter jsonAdapter, TermsOfUseRepository termsOfUseRepository) {
        this.arangoDatabase = arangoDatabase;
        this.termsOfUseRepository = termsOfUseRepository;
        this.jsonAdapter = jsonAdapter;
    }

    private ArangoCollection getPermissionsCollection() {
        ArangoDatabase database = arangoDatabase.get();
        return database.collection("permissions");
    }

    private ArangoCollection getUsersCollection() {
        ArangoDatabase database = arangoDatabase.get();
        return database.collection("users");
    }

    private ArangoCollection getInvitationsCollection() {
        ArangoDatabase database = arangoDatabase.get();
        return database.collection("invitations");
    }
    private ArangoCollection getInstanceScopesCollection() {
        ArangoDatabase database = arangoDatabase.get();
        return database.collection("instanceScopes");
    }

    private Collection<?> ensureCollection(Object o){
        if(o == null){
            return Collections.emptySet();
        }
        else if(o instanceof Collection){
            return (Collection<?>) o;
        }
        else {
            return Collections.singleton(o);
        }
    }

    @NotNull
    private Set<String> translateUserInfoToRole(String roleLabel, String key, Map<?,?> role, Map<?,?> userInfo) {
        if (!userInfo.containsKey(key)) {
            return Collections.emptySet();
        }
        Object r = role.get(key);
        Object u = userInfo.get(key);
        if (r instanceof Map && u instanceof Map) {
            return ((Map<?, ?>) r).keySet().stream()
                    .map(k -> translateUserInfoToRole(roleLabel, (String) k, (Map) r, (Map) u)).
                    flatMap(Collection::stream).collect(Collectors.toSet());
        }
        if(r!=null && u!=null){
            return ensureCollection(u).stream().filter(userClaim -> userClaim instanceof String).map(userClaim ->
                    ensureCollection(r).stream()
                            .filter(roleClaim -> ((String) userClaim).matches((String) roleClaim))
                            .map(roleClaim -> ((String)userClaim).replaceAll((String) roleClaim, roleLabel))
                            .collect(Collectors.toSet())).flatMap(Collection::stream).collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    public List<Invitation> getAllInvitationsByInstanceId(String instanceId){
        AQL aql = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        aql.add(AQL.trust("FOR i in invitations"));
        aql.addLine(AQL.trust("FILTER i.`instanceId` == @instanceId"));
        bindVars.put("instanceId", instanceId);
        aql.addLine(AQL.trust("RETURN i"));
        return arangoDatabase.get().query(aql.build().getValue(), bindVars, Invitation.class).asListRemaining();
    }

    public List<UUID> getAllInstancesWithInvitation(){
        AQL aql = new AQL();
        aql.add(AQL.trust("FOR i in invitations RETURN DISTINCT i.instanceId"));
        return arangoDatabase.get().query(aql.build().getValue(), UUID.class).asListRemaining();
    }

    public List<String> getAllInvitationsForUserId(String userId){
        AQL aql = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        aql.add(AQL.trust("FOR id IN FLATTEN(FOR i IN invitations "));
        aql.addLine(AQL.trust("FILTER i.`userId` == @userId"));
        aql.addLine(AQL.trust("LET scope = APPEND([i.`instanceId`], DOCUMENT(\"instanceScopes\", i.`instanceId`).relatedIds)"));
        aql.addLine(AQL.trust("RETURN scope) RETURN id"));
        bindVars.put("userId", userId);
        return arangoDatabase.get().query(aql.build().getValue(), bindVars, String.class).asListRemaining();
    }

    public List<Invitation> get(String userId){
        AQL aql = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        aql.add(AQL.trust("FOR i in invitations "));
        aql.addLine(AQL.trust("FILTER i.`userId` == @userId"));
        bindVars.put("userId", userId);
        aql.addLine(AQL.trust("RETURN i"));
        return arangoDatabase.get().query(aql.build().getValue(), bindVars, Invitation.class).asListRemaining();
    }

    public void createInvitation(Invitation invitation){
        getInvitationsCollection().insertDocument(jsonAdapter.toJson(invitation), new DocumentCreateOptions().overwrite(true).silent(true));
    }

    public void createOrUpdateInstanceScope(InstanceScope instanceScope){
        getInstanceScopesCollection().insertDocument(jsonAdapter.toJson(instanceScope), new DocumentCreateOptions().overwrite(true).silent(true));
    }

    public void deleteInvitation(Invitation invitation){
        getInvitationsCollection().deleteDocument(invitation.getKey());
    }


    public List<JsonLdDoc> getAllRoleDefinitions(){
        AQL aql = new AQL();
        aql.add(AQL.trust("FOR d in permissions RETURN d"));
        return arangoDatabase.get().query(aql.build().getValue(), JsonLdDoc.class).asListRemaining();
    }


    public List<String> getRolesFromUserInfo(Map<String, Object> userInfo) {
        if (userInfo == null || userInfo.isEmpty()) {
            return Collections.emptyList();
        }
        Object user = userInfo.get("sub");
        final List<JsonLdDoc> results = getAllRoleDefinitions();
        return results.stream().map(r -> {
            String role = r.get(ArangoVocabulary.KEY) != null ? (String) r.get(ArangoVocabulary.KEY) : null;
            if (role != null) {
                if (r.containsKey("authenticated") && (boolean) r.get("authenticated") && user != null) {
                    //If the role is specified to be applied whenever somebody is authenticated, we can just return it.
                    //Please note, that it is not possible to apply regex pattern for these kind of assignments.
                    return Collections.singletonList(role);
                }
                return r.keySet().stream().filter(k -> !k.startsWith("_"))
                        .map(k -> translateUserInfoToRole(role, k, r, userInfo))
                        .flatMap(Collection::stream).collect(Collectors.toSet());
            }
            return null;
        }).filter(Objects::nonNull).flatMap(Collection::stream).distinct().toList();
    }


    public JsonLdDoc getClaimForRole(Role role) {
        return getPermissionsCollection().getDocument(role.getName(), JsonLdDoc.class);
    }

    @SuppressWarnings("java:S1168") // Although we might be able to return an empty JsonLdDoc, we think it's more readable to return null
    public JsonLdDoc addClaimToRole(Role role, Map<String, Object> claimPattern) {
        JsonLdDoc document = getPermissionsCollection().getDocument(role.getName(), JsonLdDoc.class);
        if (document == null) {
            document = new JsonLdDoc();
            document.put(ArangoVocabulary.KEY, role.getName());
        }
        boolean empty = synchronizeMaps(role.getName(), claimPattern, document, false);
        if (empty) {
            getPermissionsCollection().deleteDocument(role.getName());
            return null;
        } else {
            getPermissionsCollection().insertDocument(document, new DocumentCreateOptions().overwrite(true));
            return document;
        }
    }

    @SuppressWarnings("java:S1168") // Although we might be able to return an empty JsonLdDoc, we think it's more readable to return null
    public JsonLdDoc removeClaimFromRole(Role role, Map<String, Object> claimPattern) {
        JsonLdDoc document = getPermissionsCollection().getDocument(role.getName(), JsonLdDoc.class);
        if (document == null) {
            //The document doesn't exist - nothing to remove, nothing to do...
            return null;
        }
        boolean empty = synchronizeMaps(role.getName(), claimPattern, document, true);
        if (empty) {
            getPermissionsCollection().deleteDocument(role.getName());
            return null;
        } else {
            getPermissionsCollection().insertDocument(document, new DocumentCreateOptions().overwrite(true));
            return document;
        }
    }


    private <K, V> boolean synchronizeMaps(String role, Map<K, V> source, Map<K, V> target, boolean remove) {
        Set<K> toBeRemoved = new HashSet<>();
        for (Map.Entry<K, V> entry : source.entrySet()) {
            if (!target.containsKey(entry.getKey())) {
                if (remove) {
                    //We want to remove, but it doesn't exist - so we're good.
                } else {
                    //It doesn't exist yet, so we can just attach it
                    target.put(entry.getKey(), entry.getValue());
                }
            } else {
                final Object existingValue = target.get(entry.getKey());
                if (entry.getValue() instanceof Map entryMap) {
                    if (existingValue instanceof Map existingMap)  {
                        boolean empty = synchronizeMaps(role, entryMap, existingMap, remove);
                        if (empty) {
                            target.remove(entry.getKey());
                        }
                    } else {
                        throw new RuntimeException(String.format(
                                "There is a problem with the structure of the permission map for the role %s. It seems like there are incompatible levels.", role));
                    }
                } else if (entry.getValue() instanceof List entryList) {
                    if (existingValue instanceof List existingList) {
                        for (Object e : entryList) {
                            if (remove && existingList.contains(e)) {
                                existingList.remove(e);
                            } else if (!remove && !existingList.contains(e)) {
                                existingList.add(e);
                            }
                        }
                        if (existingList.isEmpty()) {
                            toBeRemoved.add(entry.getKey());
                        }
                    } else {
                        throw new RuntimeException(String.format(
                                "There is a problem with the structure of the permission map for the role %s. It seems like there are incompatible levels.", role));
                    }
                } else if (existingValue != null && existingValue.equals(entry.getValue())){
                    toBeRemoved.add(entry.getKey());
                }
            }
        }
        toBeRemoved.forEach(target::remove);
        return target.isEmpty();
    }

    public List<UUID> getInvitationRoles(String userId){
        List<String> ids = this.getAllInvitationsForUserId(userId);
        return ids.stream().distinct().map(UUID::fromString).toList();
    }

    @Cacheable("termsOfUseByUser")
    public TermsOfUse findTermsOfUseToAccept(String userId) {
        String userDoc = getUsersCollection().getDocument(userId, String.class);
        TermsOfUseAcceptance termsOfUse;
        if (userDoc != null) {
            termsOfUse = jsonAdapter.fromJson(userDoc, TermsOfUseAcceptance.class);
        } else {
            termsOfUse = null;
        }
        TermsOfUse currentTermsOfUse = termsOfUseRepository.getCurrentTermsOfUse();
        if (currentTermsOfUse != null) {
            String currentVersion = currentTermsOfUse.getVersion();
            if (termsOfUse != null && termsOfUse.getAcceptedTermsOfUse().stream().anyMatch(t -> t.getVersion().equals(currentVersion))) {
                return null;
            }
        }
        return currentTermsOfUse;
    }

    @CacheEvict(value = "termsOfUseByUser", key = "#userId")
    public void acceptTermsOfUse(String version, String userId) {
        TermsOfUseAcceptance userAcceptance = getUsersCollection().getDocument(userId, TermsOfUseAcceptance.class);
        if (userAcceptance == null) {
            userAcceptance = new TermsOfUseAcceptance(userId, userId, new ArrayList<>());
        }
        userAcceptance.getAcceptedTermsOfUse().add(new AcceptedTermsOfUse(version, new Date()));
        getUsersCollection().insertDocument(jsonAdapter.toJson(userAcceptance), new DocumentCreateOptions().overwrite(true).silent(true));
    }

}
