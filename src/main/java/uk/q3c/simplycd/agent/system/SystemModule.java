package uk.q3c.simplycd.agent.system;

import com.google.inject.AbstractModule;
import uk.q3c.simplycd.agent.app.ExpandedPublicAddress;
import uk.q3c.simplycd.agent.app.SharedPublicAddress;

/**
 * Created by David Sowerby on 13 Jan 2017
 */
public class SystemModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(InstallationInfo.class).to(DefaultInstallationInfo.class);
        bind(RestNotifier.class).to(DefaultRestNotifier.class);
        bind(ExpandedPublicAddress.class).to(SharedPublicAddress.class).asEagerSingleton();
    }
}
