

1. DefaultRequestQueue adds a BuildRunner to the main queue
1. When queue executor invokes BuildRunner, Build is created and executed
1. Build.execute() invokes PreparationStage.execute() to prepare the workspace, clone code, connect to the Gradle build and extract Gradle KayTee configuration 
1. Part of preparation - LoadBuildConfiguration.execute() loads configuration from Gradle, calls Build.configure() to  set build up in accordance with the configuration extracted from Gradle
1. Build.configure() generates Tasks into holding queue
1. Build.execute() continues and releases the first TaskRunner into the main queue
1. The RequestQueue executor invokes the first TaskRunner, which will complete by sending either a TaskSuccessfulMessage or a TaskFailedMessage   
1. Build receives these messages via the GlobalBus.  If the task was successful, Build releases the next TaskRunner into the RequestQueue, or closes the build if all done or failed

For a delegated task (DelegatedProjectTask), step 5 calls Build.configureAsDelegate() instead of Build.configure(), with a build configuration defined by the owning project.  A delegated build never uses tasks such as bintrayUpload, and that is reflected in the configuration