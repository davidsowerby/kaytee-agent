package uk.q3c.simplycd.agent.project;

import com.google.inject.AbstractModule;
import uk.q3c.simplycd.project.DefaultProject;
import uk.q3c.simplycd.project.Project;

/**
 * Created by David Sowerby on 16 Jan 2017
 */
public class ProjectModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(Project.class).to(DefaultProject.class);
    }
}
