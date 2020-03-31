# Prepare the development environment

- Clone the kg-core repository to your local machine:
``` git clone git@github.com:HumanBrainProject/kg-core.git ```

If you are part of the KG core development team, you can additionally download the shared IntelliJ configuration providing you convenient elements such as shared run configurations, etc.:

- Navigate to the directory of the cloned repository (usually "kg-core") and clone the IntelliJ configuration (before importing the project):
``` git clone git@gitlab.humanbrainproject.org:HumanBrainProject/kg-core-ide-config.git .idea ```


- Open IntelliJ and create a new project from existing sources
- Select the root directory of your cloned kg-core repository (usually called "kg-core")
- Choose "Import project from external model" and select "Maven" -> "Finish"

- The IDE will now import the project.
- You might need to pick your "Project SDK": File -> Project Structure -> Project - choose your most recent JDK (we recommend at least 11)
- If not applied automatically, you also might need to add "Maven" framework support (right click on the root -> "Add Framework support" -> Maven. Depending on your version of IntelliJ, you can also add Spring support (optional)
- It's a good idea to enable the Maven auto-import
Now you should be able to see a project structure looking like
  - **kg-core**: The root directory containing some scripts organizing the build / dockerization of the different services
    - **adapters**: Adapter modules allowing the ingestion of external systems
    - **config**: Shared configurations of the services (e.g. parent poms)
    - **libs**: A collection of common libraries (shared models, shared functionalities
    - **libs-4-test**: A collection of convenience libraries for testing
    - **mocks**: A collection for mocked services (to simplify and parallelize development / allow testing without launching the full environment) 
    - **services**: A collection of the actual services composing the KG-core environment and therefore containing the "real logic" of the system
    - **tests**: System tests which can be deployed as services themselves.

# Run the code (development mode)
If you have cloned the IntelliJ configuration, you will see several run configurations ready for you to be executed with default settings. Otherwise, every service is a standard Spring Boot application which can be launched accordingly. Please note, that the services have some dependencies: All of them resolve their peers through the kg-service-discovery - so it's always a good choice to run this one in any case. 
