package uk.q3c.kaytee.agent.build

import org.gradle.tooling.*
import org.gradle.tooling.events.OperationType
import org.gradle.tooling.model.Launchable
import org.gradle.tooling.model.Task

import java.time.LocalDateTime

/**
 * Created by David Sowerby on 28 Jan 2017
 */
class MockGradleLauncher implements BuildLauncher {
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
        TaskRandomiser randomiser = new TaskRandomiser()
        randomiser.calculate()
        println "GRADLE TASK RUNNING for $randomiser.duration ms"
//                println "mocking GradleTaskRequest, task: $taskKey, duration: $randomiser.duration, fail: $randomiser.fail"
//        Thread.sleep(randomiser.duration)

        //simulate doing something rather than just sleeping
        int d = randomiser.duration
        while (d > 0) {
            d--
        }


        if (randomiser.fail) {
//            println "task failed throw exception"
//            throw randomiser.failureException
        } else {
            "task has completed successfully"
        }

    }

    @Override
    void run(ResultHandler<? super Void> handler) throws IllegalStateException {


        Thread t = new Thread(new Runnable() {

            @Override
            void run() {
                LocalDateTime start = LocalDateTime.now()
                TaskRandomiser randomiser = new TaskRandomiser()
                randomiser.calculate()
                println "GRADLE TASK RUNNING for $randomiser.duration ms"
//                println "mocking GradleTaskRequest, task: $taskKey, duration: $randomiser.duration, fail: $randomiser.fail"
                Thread.sleep(randomiser.duration)
                LocalDateTime end = LocalDateTime.now()
                if (randomiser.fail) {
                    println "notify build, task failed"
                    handler.onFailure(randomiser.failureException)
                } else {
                    println "notify build, task has completed successfully"
//                    new BuildResult(start,end,BuildResultStateKey.Build_Successful)
                    handler.onComplete(new Object())
                }

            }
        })
        t.run()
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
