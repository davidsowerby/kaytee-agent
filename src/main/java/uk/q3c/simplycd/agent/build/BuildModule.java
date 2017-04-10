package uk.q3c.simplycd.agent.build;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import uk.q3c.simplycd.agent.queue.BuildRunner;
import uk.q3c.simplycd.agent.queue.DefaultBuildRunner;

/**
 * Created by David Sowerby on 07 Jan 2017
 */
@SuppressWarnings("ClassWithoutLogger")
public class BuildModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(BuildRecordCollator.class).to(DefaultBuildRecordCollator.class).asEagerSingleton();
        bind(BuildRecordService.class).to(DefaultBuildRecordService.class);
        bind(BuildNumberReader.class).to(DefaultBuildNumberReader.class);
        install(new FactoryModuleBuilder()
                .implement(BuildRunner.class, DefaultBuildRunner.class)
                .build(BuildRequestFactory.class));

        install(new FactoryModuleBuilder()
                .implement(Build.class, DefaultBuild.class)
                .build(BuildFactory.class));

    }
}
