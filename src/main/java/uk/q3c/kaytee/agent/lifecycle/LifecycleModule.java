package uk.q3c.kaytee.agent.lifecycle;

import com.google.inject.AbstractModule;
import uk.q3c.kaytee.agent.prepare.*;

/**
 * Created by David Sowerby on 17 Jan 2017
 */
public class LifecycleModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(PreparationStage.class).to(DefaultPreparationStage.class);
        bind(GitClone.class).to(DefaultGitClone.class);
        bind(PrepareWorkspace.class).to(DefaultPrepareWorkspace.class);
        bind(LoadBuildConfiguration.class).to(DefaultLoadBuildConfiguration.class);
        bind(ConnectBuildToGradle.class).to(DefaultConnectBuildToGradle.class);
    }
}
