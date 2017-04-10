package uk.q3c.simplycd.agent.system;

import com.google.inject.AbstractModule;
import ratpack.server.PublicAddress;
import uk.q3c.simplycd.agent.app.SharedPublicAddress;

/**
 * Created by David Sowerby on 13 Jan 2017
 */
public class SystemModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(InstallationInfo.class).to(DefaultInstallationInfo.class);
        bind(RestNotifier.class).to(DefaultRestNotifier.class);
        bind(PublicAddress.class).to(SharedPublicAddress.class).asEagerSingleton();
    }
}
