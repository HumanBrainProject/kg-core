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

package eu.ebrains.kg.graphdb.instances.controller;

import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.arango.commons.model.ArangoDocumentReference;
import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.exception.ForbiddenException;
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.markers.ExposesData;
import eu.ebrains.kg.commons.markers.ExposesMinimalData;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.commons.model.Type;
import eu.ebrains.kg.commons.permission.Functionality;
import eu.ebrains.kg.commons.permissions.controller.Permissions;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;
import eu.ebrains.kg.graphdb.commons.controller.ArangoDatabases;
import eu.ebrains.kg.graphdb.commons.model.ArangoDocument;
import eu.ebrains.kg.graphdb.structure.controller.MetaDataController;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

@Component
public class InstancesRepository extends AbstractRepository {

    private final Permissions permissions;
    private final AuthContext authContext;
    private final DocumentsRepository documents;
    private final EmbeddedAndAlternativesRepository embeddedAndAlternatives;
    private final MetaDataController metaDataController;
    private final IncomingLinksRepository incomingLinks;
    private final ArangoDatabases databases;

    public InstancesRepository(Permissions permissions, AuthContext authContext, DocumentsRepository documents, EmbeddedAndAlternativesRepository embeddedAndAlternatives, MetaDataController metaDataController, IncomingLinksRepository incomingLinks, ArangoDatabases databases) {
        this.permissions = permissions;
        this.authContext = authContext;
        this.documents = documents;
        this.embeddedAndAlternatives = embeddedAndAlternatives;
        this.metaDataController = metaDataController;
        this.incomingLinks = incomingLinks;
        this.databases = databases;
    }

    @ExposesData
    public NormalizedJsonLd getInstance(DataStage stage, SpaceName space, UUID id, boolean embedded, boolean removeInternalProperties, boolean alternatives, boolean showIncomingLinks, Long incomingLinksPageSize) {
        if (!permissions.hasPermission(authContext.getUserWithRoles(), Functionality.MINIMAL_READ, space, id)) {
            throw new ForbiddenException(String.format("You don't have read rights on the instance with the id %s", id));
        }
        ArangoDocument document = documents.getDocument(stage, ArangoCollectionReference.fromSpace(space).doc(id));
        if (document == null) {
            return null;
        }
        List<NormalizedJsonLd> singleDoc = Collections.singletonList(document.getDoc());
        embeddedAndAlternatives.handleAlternativesAndEmbedded(singleDoc, stage, alternatives, embedded);
        exposeRevision(singleDoc);

        final List<NormalizedJsonLd> invitationDocuments = documents.getInvitationDocuments();
        if (showIncomingLinks) {
            ArangoDocumentReference arangoDocumentReference = ArangoDocumentReference.fromInstanceId(new InstanceId(id, space));
            final boolean ignoreIncomingLinks = metaDataController.getTypesByName(document.getDoc().types(), stage, space.getName(), false, false, authContext.getUserWithRoles(), authContext.getClientSpace()!=null ? authContext.getClientSpace().getName() : null, invitationDocuments).values().stream().filter(t -> t.getData() != null).map(t -> Type.fromPayload(t.getData())).anyMatch(t -> t.getIgnoreIncomingLinks() != null && t.getIgnoreIncomingLinks());
            if (!ignoreIncomingLinks) {
                NormalizedJsonLd instanceIncomingLinks = incomingLinks.fetchIncomingLinks(Collections.singletonList(arangoDocumentReference), stage, 0L, incomingLinksPageSize, null, null);
                if (!CollectionUtils.isEmpty(instanceIncomingLinks)) {
                    incomingLinks.resolveIncomingLinks(stage, instanceIncomingLinks, invitationDocuments);
                    NormalizedJsonLd d = document.getDoc();
                    d.put(EBRAINSVocabulary.META_INCOMING_LINKS, instanceIncomingLinks.get(id.toString()));
                }
            }
        }
        if (removeInternalProperties) {
            document.getDoc().removeAllInternalProperties();
        }
        NormalizedJsonLd doc = document.getDoc();
        if (doc != null && !permissions.hasPermission(authContext.getUserWithRoles(), stage == DataStage.RELEASED ? Functionality.READ_RELEASED : Functionality.READ, space, id)) {
            //The user doesn't have read rights - we need to restrict the information to minimal data
            doc.keepPropertiesOnly(documents.getMinimalFields(stage, doc.types(), invitationDocuments));
        }
        return doc;
    }


    @ExposesMinimalData
    public Map<UUID, String> getLabelsForInstances(DataStage stage, Set<InstanceId> ids) {
        return getLabelsForInstances(stage, ids, databases);
    }

}
