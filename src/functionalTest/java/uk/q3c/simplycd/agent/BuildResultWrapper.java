package uk.q3c.simplycd.agent;

import uk.q3c.simplycd.agent.build.BuildResult;
import uk.q3c.simplycd.agent.build.TaskResult;
import uk.q3c.simplycd.agent.i18n.TaskKey;

/**
 * Quirky, and annoying, Groovy behaviour refuses to recognise a method with TaskKey parameter, or just about anything else with TaskKey
 * <p>
 * Created by David Sowerby on 27 Mar 2017
 */
public class BuildResultWrapper {
    private BuildResult buildResult;

    public BuildResultWrapper(BuildResult buildResult) {
        this.buildResult = buildResult;
    }

    public TaskResult taskResult(String keyName) {
        TaskKey key = TaskKey.valueOf(keyName);
        return buildResult.taskResult(key);
    }


}
