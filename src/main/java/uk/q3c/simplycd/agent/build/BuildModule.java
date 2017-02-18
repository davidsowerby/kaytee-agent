package uk.q3c.simplycd.agent.build;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import uk.q3c.simplycd.build.*;
import uk.q3c.simplycd.queue.BuildRequest;
import uk.q3c.simplycd.queue.DefaultBuildRequest;

/**
 * Created by David Sowerby on 07 Jan 2017
 */
@SuppressWarnings("ClassWithoutLogger")
public class BuildModule extends AbstractModule {
    @Override
    protected void configure() {


        bind(BuildNumberReader.class).to(DefaultBuildNumberReader.class);
        install(new FactoryModuleBuilder()
                .implement(BuildRequest.class, DefaultBuildRequest.class)
                .build(BuildRequestFactory.class));

        install(new FactoryModuleBuilder()
                .implement(Build.class, DefaultBuild.class)
                .build(BuildFactory.class));

    }
}
