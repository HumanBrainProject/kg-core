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

package eu.ebrains.kg.nexusv1.controller;

import com.netflix.discovery.EurekaClient;
import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.AuthTokens;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.jsonld.*;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Event;
import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.commons.permission.ClientAuthToken;
import eu.ebrains.kg.commons.permission.UserAuthToken;
import eu.ebrains.kg.commons.semantics.vocabularies.SchemaOrgVocabulary;
import eu.ebrains.kg.docker.SpringDockerComposeRunner;
import eu.ebrains.kg.nexusv1.models.NexusV1Event;
import eu.ebrains.kg.nexusv1.serviceCall.IdsSvc;
import eu.ebrains.kg.nexusv1.serviceCall.NexusSvc;
import eu.ebrains.kg.test.GraphDBSyncSvcForTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class EventDispatcherTest {

    @Autowired
    EurekaClient discoveryClient;

    @Autowired
    EventDispatcher eventDispatcher;

    @Autowired
    IdUtils idUtils;

    @Autowired
    GraphDBSyncSvcForTest graphDBSyncSvcForTest;

    @MockBean
    NexusSvc nexusSvcMock;

    @MockBean
    AuthContext authContext;

    @Autowired
    IdsSvc idsSvc;

    SpringDockerComposeRunner dockerComposeRunner;


    String space = "test";
    String userId = "TestUser";

    String id = "http://myresources.com/123";
    JsonLdDoc orgData = new JsonLdDoc();
    JsonLdId documentId = new JsonLdId(id);

    public NormalizedJsonLd generateEventData(){
        NormalizedJsonLd jsonLd = new NormalizedJsonLd();
        jsonLd.addProperty("_organizationUuid", space);
        jsonLd.addProperty("_instant", "2020-01-01T20:20:20.4444Z");
        jsonLd.addProperty("_subject", "test");
        jsonLd.addProperty("_resourceId",id);
        return  jsonLd;
    }

    @Before
    public void setup() {
        this.dockerComposeRunner = new SpringDockerComposeRunner(discoveryClient, true, false, Arrays.asList("arango"), "kg-inference", "kg-primarystore","kg-permissions", "kg-graphdb-sync", "kg-indexing", "kg-jsonld", "kg-ids");
        dockerComposeRunner.start();

    }

    @Test
    public void testDispatchInsertEvent(){
        when(authContext.getAuthTokens()).thenReturn(new AuthTokens(new UserAuthToken(""), new ClientAuthToken("")));
        orgData.addProperty("_label","test");
        //Given
        when(nexusSvcMock.getOrgInfo(any(String.class), any(ClientAuthToken.class))).thenReturn(orgData);

        UUID eventId = UUID.randomUUID();
        NormalizedJsonLd jsonLd = generateEventData();

        NexusV1Event e = new NexusV1Event(new Space(space), UUID.randomUUID(), jsonLd, Event.Type.INSERT, new Date(), NexusV1Event.Type.Created, eventId.toString());
        eventDispatcher.dispatchEvent(e);
        JsonLdIdMapping resolvedNativeId = idsSvc.resolveId(DataStage.NATIVE, documentId.getId());
        JsonLdIdMapping resolvedLiveId = idsSvc.resolveId(DataStage.LIVE, documentId.getId());

        //When
        IndexedJsonLdDoc instance = graphDBSyncSvcForTest.get(DataStage.NATIVE, resolvedNativeId.getSpace(), idUtils.getUUID(resolvedNativeId.getResolvedIds().iterator().next()));
        IndexedJsonLdDoc instanceLive = graphDBSyncSvcForTest.get(DataStage.LIVE, resolvedLiveId.getSpace(), idUtils.getUUID(resolvedLiveId.getResolvedIds().iterator().next()));

        // Then
        assertNotNull(instance);
        assertNotNull(instanceLive);
    }


    @Test
    public void testDispatchUpdatetEvent(){
        //Given
        when(authContext.getAuthTokens()).thenReturn(new AuthTokens(new UserAuthToken(""), new ClientAuthToken("")));
        orgData.addProperty("_label","test");
        when(nexusSvcMock.getOrgInfo(any(String.class), any(ClientAuthToken.class))).thenReturn(orgData);
        String originalData =  "My Test Name";
        String exptectedData = "Corrected name";
        String sourceId = "http://myid/123";
        NormalizedJsonLd jsonLd = generateEventData();
        NormalizedJsonLd sourceData = new NormalizedJsonLd();
        sourceData.addProperty(SchemaOrgVocabulary.NAME, originalData);
        sourceData.addProperty(JsonLdConsts.ID, sourceId);
        jsonLd.addProperty("_source", sourceData);
        UUID insertEventId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();
        NexusV1Event e = new NexusV1Event(new Space(space), instanceId, jsonLd, Event.Type.INSERT, new Date(), NexusV1Event.Type.Created, insertEventId.toString());
        eventDispatcher.dispatchEvent(e);
        JsonLdIdMapping jsonLdIdMapping = idsSvc.resolveId(DataStage.LIVE, documentId.getId());
        IndexedJsonLdDoc instanceLive = graphDBSyncSvcForTest.get(DataStage.LIVE, jsonLdIdMapping.getSpace(), idUtils.getUUID(jsonLdIdMapping.getResolvedIds().iterator().next()));
        String  defaultData=  (String) instanceLive.getDoc().get(SchemaOrgVocabulary.NAME);
        assertEquals(defaultData, originalData);

        //When
        NormalizedJsonLd updatedEventData = generateEventData();
        NormalizedJsonLd newSourceData = new NormalizedJsonLd();
        newSourceData.addProperty(SchemaOrgVocabulary.NAME, exptectedData);
        newSourceData.addProperty(JsonLdConsts.ID, sourceId);
        updatedEventData.addProperty("_source", newSourceData);
        UUID updateEventId = UUID.randomUUID();
        NexusV1Event updateEvent = new NexusV1Event(new Space(space), instanceId, updatedEventData, Event.Type.UPDATE, new Date(), NexusV1Event.Type.Updated, updateEventId.toString());
        eventDispatcher.dispatchEvent(updateEvent);

        // Then
        IndexedJsonLdDoc updatedInstance = graphDBSyncSvcForTest.get(DataStage.LIVE,  jsonLdIdMapping.getSpace(), idUtils.getUUID(jsonLdIdMapping.getResolvedIds().iterator().next()));
        String  updatedData =  (String) updatedInstance.getDoc().get(SchemaOrgVocabulary.NAME);
        assertEquals(updatedData, exptectedData);
    }

    @Test
    public void testDispatchDeleteEvent(){
        //Given
        when(authContext.getAuthTokens()).thenReturn(new AuthTokens(new UserAuthToken(""), new ClientAuthToken("")));
        orgData.addProperty("_label","test");
        when(nexusSvcMock.getOrgInfo(any(String.class), any(ClientAuthToken.class))).thenReturn(orgData);
        String sourceId = "http://myid/123";
        NormalizedJsonLd jsonLd = generateEventData();
        NormalizedJsonLd sourceData = new NormalizedJsonLd();
        sourceData.addProperty(JsonLdConsts.ID, sourceId);
        jsonLd.addProperty("_source", sourceData);
        UUID insertEventId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();
        NexusV1Event e = new NexusV1Event(new Space(space), instanceId, jsonLd, Event.Type.INSERT, new Date(), NexusV1Event.Type.Created, insertEventId.toString());
        eventDispatcher.dispatchEvent(e);
        JsonLdIdMapping resolvedLiveId = idsSvc.resolveId(DataStage.LIVE, documentId.getId());
        JsonLdIdMapping resolvedNativeId = idsSvc.resolveId(DataStage.NATIVE, documentId.getId());
        IndexedJsonLdDoc instanceLive = graphDBSyncSvcForTest.get(DataStage.LIVE, resolvedLiveId.getSpace(), idUtils.getUUID(resolvedLiveId.getResolvedIds().iterator().next()));
        assertNotNull(instanceLive);

        //When
        NormalizedJsonLd emptySource = generateEventData();
        emptySource.addProperty("_source", sourceData);
        UUID deleteEventUUID = UUID.randomUUID();
        NexusV1Event deleteEvent = new NexusV1Event(new Space(space), instanceId, emptySource, Event.Type.DELETE, new Date(), NexusV1Event.Type.Deprecated, deleteEventUUID.toString());
        eventDispatcher.dispatchEvent(deleteEvent);

        // Then
        IndexedJsonLdDoc deletedInstanceNative = graphDBSyncSvcForTest.get(DataStage.NATIVE,  resolvedNativeId.getSpace(), idUtils.getUUID(resolvedNativeId.getResolvedIds().iterator().next()));
        IndexedJsonLdDoc deletedInstance = graphDBSyncSvcForTest.get(DataStage.LIVE, resolvedLiveId.getSpace(), idUtils.getUUID(resolvedLiveId.getResolvedIds().iterator().next()));

        assertNull(deletedInstanceNative);
        assertNull(deletedInstance);
    }

}
