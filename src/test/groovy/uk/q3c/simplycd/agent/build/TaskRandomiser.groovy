package uk.q3c.simplycd.agent.build

import org.gradle.tooling.BuildCancelledException
import org.gradle.tooling.BuildException
import org.gradle.tooling.GradleConnectionException
import org.gradle.tooling.UnsupportedVersionException
import org.gradle.tooling.exceptions.UnsupportedBuildArgumentException
import org.gradle.tooling.exceptions.UnsupportedOperationConfigurationException

/**
 * Created by David Sowerby on 29 Jan 2017
 */
class TaskRandomiser {

    final int maxDuration = 1000
    final int minDuration = 100
    int failureRate = 5  // for example 1 in every 5 fails
    int exceptionTypes = 6 // change this if we add an expected exception

    int duration
    boolean fail
    GradleConnectionException failureException = null

    void calculate() {
        Random random = new Random()
        duration = random.nextInt(maxDuration - minDuration) + minDuration

        int failCheck = random.nextInt(failureRate)
        fail = failCheck == 0

        if (fail) {
            failureException = selectException(random.nextInt(exceptionTypes))
        }
    }

    GradleConnectionException selectException(int index) {
        String msg = "randomised failure";
        switch (index) {
            case 0: return new UnsupportedOperationConfigurationException(msg); break;
            case 1: return new UnsupportedVersionException(msg); break;

            case 2: return new BuildException(msg, new NullPointerException(msg)); break;
            case 3: return new BuildCancelledException(msg); break;

            case 4: return new UnsupportedBuildArgumentException(msg); break;
            case 5: return new GradleConnectionException(msg); break;

        }
    }
}


