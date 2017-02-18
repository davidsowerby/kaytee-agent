package uk.q3c.simplycd.agent.i18n;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import uk.q3c.simplycd.i18n.DefaultNamed;
import uk.q3c.simplycd.i18n.Named;
import uk.q3c.simplycd.i18n.NamedFactory;

/**
 * Created by David Sowerby on 21 Jan 2017
 */
public class I18NModule extends AbstractModule {
    @Override
    protected void configure() {
        install(new FactoryModuleBuilder()
                .implement(Named.class, DefaultNamed.class)
                .build(NamedFactory.class));
    }
}
