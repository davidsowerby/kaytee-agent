package uk.q3c.simplycd.agent.i18n;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import uk.q3c.krail.core.i18n.CurrentLocale;
import uk.q3c.krail.core.i18n.Translate;
import uk.q3c.simplycd.i18n.Named;

/**
 * Created by David Sowerby on 21 Jan 2017
 */
public class I18NModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(CurrentLocale.class).to(SpecifiedCurrentLocale.class);
        bind(Translate.class).to(LocalTranslate.class);
        install(new FactoryModuleBuilder()
                .implement(Named.class, DefaultNamed.class)
                .build(NamedFactory.class));
    }
}
