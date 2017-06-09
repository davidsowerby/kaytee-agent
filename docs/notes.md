

1. DefaultRequestQueue adds a BuildRunner to the main queue
1. When queue executor invokes BuildRunner, Build is created and executed
1. Build.execute() invokes PreparationStage.execute() to prepare the workspace, clone code, connect to the Gradle build and extract Gradle KayTee configuration 
1. Part of preparation - LoadBuildConfiguration.execute() loads configuration from Gradle, calls Build.configure() to  set build up in accordance with the configuration extracted from Gradle
1. Build.configure() generates Tasks into holding queue
1. Build.execute() continues and releases the first TaskRunner into the main queue
1. The RequestQueue executor invokes the first TaskRunner, which will complete by sending either a TaskSuccessfulMessage or a TaskFailedMessage   
1. Build receives these messages via the GlobalBus.  If the task was successful, Build releases the next TaskRunner into the RequestQueue, or closes the build if all done or failed
1. The BuildCollator also monitors the GlobalBus for all build messages and collates a BuildRecord for each Build

For a delegated task (DelegatedProjectTask) ...tbd

DelegateProjectTaskRunner waits for BuildSuccessful or BuildFailedMessage for its delegate build and issues a TaskCompletedMessage or TaskFailedMessage as appropriate