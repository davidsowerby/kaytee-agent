package uk.q3c.kaytee.agent.queue;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import uk.q3c.kaytee.agent.build.BuildRequestFactory;

/**
 * Created by David Sowerby on 06 May 2016
 */
@SuppressWarnings({"ClassWithoutLogger", "AnonymousInnerClassMayBeStatic", "AnonymousInnerClass", "EmptyClass"})
public class QueueModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ManualTaskLauncher.class).to(DefaultManualTaskLauncher.class);
        bind(RequestQueue.class).to(DefaultRequestQueue.class);
        bind(GradleTaskExecutor.class).to(DefaultGradleTaskExecutor.class);

        install(new FactoryModuleBuilder()
                .implement(BuildRunner.class, DefaultBuildRunner.class)
                .build(BuildRequestFactory.class));

        install(new FactoryModuleBuilder()
                .implement(GradleTaskRunner.class, DefaultGradleTaskRunner.class)
                .build(GradleTaskRunnerFactory.class));

        install(new FactoryModuleBuilder()
                .implement(ManualTaskRunner.class, DefaultManualTaskRunner.class)
                .build(ManualTaskRunnerFactory.class));
    }


}


//        final TypeLiteral<LinkedBlockingQueue<BuildRequest>> queueLiteral = new TypeLiteral<LinkedBlockingQueue<BuildRequest>>(){
//        };
//
//        bind(queueLiteral).toInstance(new LinkedBlockingQueue<>()); //implicitly makes this a singleton
