# Release API

```puml
actor Client
Client -> ReleaseApi: Release Instance Http Request
ReleaseApi -> ReleaseController: Release Request (DocumentId docId, String revision)
ReleaseController -> GraphDBSync: Get Document Request(Stage stage, DocumentId docId)
GraphDBSync -> ReleaseController: Get Document Response
ReleaseController -> PrimaryStore: Release Event Request
PrimaryStore -> GraphDBSync: Release instance Request
GraphDBSync -> PrimaryStore: Release instance Response
PrimaryStore -> GraphDBSync: Instance Status Modification Request
GraphDBSync -> PrimaryStore: Instance Status Modification Response
PrimaryStore -> ReleaseController: Release Event Response
ReleaseController -> ReleaseApi: Release Response
ReleaseApi -> Client: Release Instance Http Response
```