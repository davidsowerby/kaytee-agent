package uk.q3c.kaytee.agent.i18n;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import uk.q3c.krail.core.i18n.CurrentLocale;
import uk.q3c.krail.core.i18n.Translate;
import uk.q3c.krail.core.persist.clazz.i18n.ClassPatternDao;
import uk.q3c.krail.core.persist.clazz.i18n.ClassPatternSource;
import uk.q3c.krail.core.persist.common.i18n.PatternDao;

/**
 * Created by David Sowerby on 21 Jan 2017
 */
public class I18NModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(CurrentLocale.class).to(SpecifiedCurrentLocale.class).in(Singleton.class);
        bind(Translate.class).to(LocalTranslate.class);
//        bindPatternSource();
        bindClassPatternDao();
        bindPatternDao();

        install(new FactoryModuleBuilder()
                .implement(Named.class, DefaultNamed.class)
                .build(NamedFactory.class));
    }

    /**
     * Binds the {@link ClassPatternDao} to its default implementation, override to provide your own implementation
     */
    protected void bindClassPatternDao() {
        bind(ClassPatternDao.class).to(LocalClassPatternDao.class);
    }

    /**
     * Binds the {@link PatternDao} to the annotation for {@link ClassPatternDao}.   This enables class based I18N patterns to be used, if {@link
     * ClassPatternSource} is included within I18NModule as a source.
     */
    @SuppressWarnings("UninstantiableBinding") // fooled by bindClassPatternDao causing indirection
    protected void bindPatternDao() {
        bind(PatternDao.class).annotatedWith(ClassPatternSource.class)
                .to(ClassPatternDao.class);

    }

//    /**
//     * It is generally advisable to use the same scope for this as for current locale.   See javadoc for {@link
//     * DefaultPatternSource} for an explanation of what this is for.  Override this method if you provide your own implementation
//     */
//    protected void bindPatternSource() {
//        bind(PatternSource.class).to(DefaultPatternSource.class)
//                .in(Singleton.class);
//    }

}
