package uk.q3c.kaytee.agent.build

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import uk.q3c.kaytee.agent.eventbus.GlobalBusProvider
import uk.q3c.kaytee.agent.i18n.TaskStateKey
import uk.q3c.kaytee.agent.queue.GradleTaskRunner
import uk.q3c.kaytee.agent.queue.TaskFailedMessage
import uk.q3c.kaytee.agent.queue.TaskStartedMessage
import uk.q3c.kaytee.agent.queue.TaskSuccessfulMessage
import uk.q3c.kaytee.plugin.TaskKey
/**
 * Created by David Sowerby on 09 Jul 2017
 */
class MockGradleTaskRunner implements GradleTaskRunner {
    Build build
    TaskKey taskKey
    boolean includeQualityGate
    private GlobalBusProvider globalBusProvider
    private Logger log = LoggerFactory.getLogger(this.getClass().name)
    boolean failOnRun = false

    MockGradleTaskRunner(GlobalBusProvider globalBusProvider,
                         Build build, TaskKey taskKey, boolean includeQualityGate) {
        this.globalBusProvider = globalBusProvider
        this.build = build
        this.taskKey = taskKey
        this.includeQualityGate = includeQualityGate
    }

    @Override
    String identity() {
        return "Mock Gradle task runner for $taskKey, includeQualityGate is: $includeQualityGate"
    }

    @Override
    void run() {
        log.debug("publishing TaskStartedMessage for {}", this)
        TaskStartedMessage startMessage = new TaskStartedMessage(this.build.buildRunner.uid, taskKey, build.buildRunner.delegated)
        globalBusProvider.get().publishAsync(startMessage)
//        LocalDateTime timeout = LocalDateTime.now().plus(500, ChronoUnit.MILLIS)
//        while (LocalDateTime.now().isBefore(timeout)) {
//            int a = 10 + 1000
//            int b = a * 50
//        }
        if (failOnRun) {
            log.info("$taskKey Task FAILED")
            TaskFailedMessage failedMessage = new TaskFailedMessage(build.buildRunner.uid, taskKey, build.buildRunner.delegated, TaskStateKey.Failed, "Failed stdout", "Failed stderr", new RuntimeException("Fake"))
            globalBusProvider.get().publishAsync(failedMessage)
        } else {
            log.info("$taskKey Task SUCCEEDED")
            TaskSuccessfulMessage successfulMessage = new TaskSuccessfulMessage(build.buildRunner.uid, taskKey, build.buildRunner.delegated, "Successful")
            globalBusProvider.get().publishAsync(successfulMessage)
        }
    }
}
