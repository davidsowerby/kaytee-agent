package uk.q3c.kaytee.agent.build

import org.gradle.tooling.*
import org.gradle.tooling.events.OperationType
import org.gradle.tooling.model.Launchable
import org.gradle.tooling.model.Task

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Provides a task with fixed duration.  Task fails if it is in failTasks
 *
 * Created by David Sowerby on 28 Jan 2017
 */
class MockGradleLauncher2 implements BuildLauncher {
    List<String> failTasks = new ArrayList<>()


    @Override
    BuildLauncher forTasks(String... tasks) {
        return this
    }

    @Override
    BuildLauncher forTasks(Task... tasks) {
        return this
    }

    @Override
    BuildLauncher forTasks(Iterable<? extends Task> tasks) {
        return this
    }

    @Override
    BuildLauncher forLaunchables(Launchable... launchables) {
        return this
    }

    @Override
    BuildLauncher forLaunchables(Iterable<? extends Launchable> launchables) {
        return this
    }

    @Override
    void run() throws GradleConnectionException, IllegalStateException {
        int taskInMillis = 200
        LocalDateTime taskEnd = LocalDateTime.now().plus(taskInMillis, ChronoUnit.MILLIS)

        println "GRADLE TASK RUNNING for $taskInMillis ms"

        //simulate doing something rather than just sleeping
        int d = 10000
        while (LocalDateTime.now().isBefore(taskEnd)) {
            d--
        }


        if (failTasks.contains()) {
            println "task failed throw exception"
            throw new IOException("fake")
        } else {
            "task has completed successfully"
        }

    }

    @Override
    void run(ResultHandler<? super Void> handler) throws IllegalStateException {

        throw new RuntimeException("TODO");
    }

    @Override
    BuildLauncher withArguments(String... arguments) {
        return this
    }

    @Override
    BuildLauncher withArguments(Iterable<String> arguments) {
        return this
    }

    @Override
    BuildLauncher setStandardOutput(OutputStream outputStream) {
        return this
    }

    @Override
    BuildLauncher setStandardError(OutputStream outputStream) {
        return this
    }

    @Override
    BuildLauncher setColorOutput(boolean colorOutput) {
        return this
    }

    @Override
    BuildLauncher setStandardInput(InputStream inputStream) {
        return this
    }

    @Override
    BuildLauncher setJavaHome(File javaHome) {
        return this
    }

    @Override
    BuildLauncher setJvmArguments(String... jvmArguments) {
        return this
    }

    @Override
    BuildLauncher setJvmArguments(Iterable<String> jvmArguments) {
        return null
    }

    @Override
    BuildLauncher addProgressListener(ProgressListener listener) {
        return null
    }

    @Override
    BuildLauncher addProgressListener(org.gradle.tooling.events.ProgressListener listener) {
        return null
    }

    @Override
    BuildLauncher addProgressListener(org.gradle.tooling.events.ProgressListener listener, Set<OperationType> eventTypes) {
        return null
    }

    @Override
    BuildLauncher addProgressListener(org.gradle.tooling.events.ProgressListener listener, OperationType... operationTypes) {
        return null
    }

    @Override
    BuildLauncher withCancellationToken(CancellationToken cancellationToken) {
        return null
    }
}
