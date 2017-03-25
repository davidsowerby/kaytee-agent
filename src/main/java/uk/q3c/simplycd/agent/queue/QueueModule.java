package uk.q3c.simplycd.agent.queue;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import uk.q3c.simplycd.agent.build.BuildRequestFactory;

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
                .implement(BuildRequest.class, DefaultBuildRequest.class)
                .build(BuildRequestFactory.class));

        install(new FactoryModuleBuilder()
                .implement(GradleTaskRequest.class, DefaultGradleTaskRequest.class)
                .build(GradleTaskRequestFactory.class));

        install(new FactoryModuleBuilder()
                .implement(ManualTaskRequest.class, DefaultManualTaskRequest.class)
                .build(ManualTaskRequestFactory.class));
    }


}


//        final TypeLiteral<LinkedBlockingQueue<BuildRequest>> queueLiteral = new TypeLiteral<LinkedBlockingQueue<BuildRequest>>(){
//        };
//
//        bind(queueLiteral).toInstance(new LinkedBlockingQueue<>()); //implicitly makes this a singleton
