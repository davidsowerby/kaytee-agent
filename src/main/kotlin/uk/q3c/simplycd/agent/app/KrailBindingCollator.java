package uk.q3c.simplycd.agent.app;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.servlet.ServletModule;
import com.google.inject.util.Modules;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.bval.guice.ValidationModule;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.realm.Realm;
import uk.q3c.krail.core.config.ApplicationConfigurationModule;
import uk.q3c.krail.core.data.DataModule;
import uk.q3c.krail.core.eventbus.EventBusModule;
import uk.q3c.krail.core.guice.BaseServletModule;
import uk.q3c.krail.core.guice.DefaultBindingManager;
import uk.q3c.krail.core.guice.threadscope.ThreadScopeModule;
import uk.q3c.krail.core.i18n.I18NModule;
import uk.q3c.krail.core.navigate.NavigationModule;
import uk.q3c.krail.core.navigate.sitemap.MasterSitemap;
import uk.q3c.krail.core.navigate.sitemap.SitemapModule;
import uk.q3c.krail.core.navigate.sitemap.StandardPagesModule;
import uk.q3c.krail.core.option.InMemory;
import uk.q3c.krail.core.option.OptionModule;
import uk.q3c.krail.core.persist.inmemory.common.InMemoryModule;
import uk.q3c.krail.core.push.PushModule;
import uk.q3c.krail.core.services.ServicesModule;
import uk.q3c.krail.core.shiro.DefaultShiroModule;
import uk.q3c.krail.core.shiro.ShiroVaadinModule;
import uk.q3c.krail.core.shiro.aop.KrailShiroAopModule;
import uk.q3c.krail.core.ui.DataTypeModule;
import uk.q3c.krail.core.user.UserModule;
import uk.q3c.krail.core.validation.KrailValidationModule;
import uk.q3c.krail.core.view.ViewModule;
import uk.q3c.krail.core.view.component.DefaultComponentModule;
import uk.q3c.krail.testutil.guice.uiscope.TestUIScopeModule;
import uk.q3c.krail.testutil.guice.vsscope.TestVaadinSessionScopeModule;
import uk.q3c.krail.testutil.i18n.TestI18NModule;
import uk.q3c.krail.testutil.ui.TestUIModule;
import uk.q3c.krail.util.UtilsModule;

import java.util.ArrayList;
import java.util.List;

/**
 * A temporary solution pendoing resolution of: https://github.com/davidsowerby/krail/issues/581
 * <p>
 * This is a copy of the code from {@link DefaultBindingManager}
 * <p>
 * Created by David Sowerby on 18 Jan 2017
 */
public class KrailBindingCollator {

    public List<Module> modules() {
        List<Module> coreModules = new ArrayList<>(30);

        coreModules.add(uiModule());
        coreModules.add(i18NModule());
        coreModules.add(applicationConfigurationModule());
        coreModules.add(new SitemapModule());

        coreModules.add(new ThreadScopeModule());
        coreModules.add(new TestUIScopeModule());
        coreModules.add(new TestVaadinSessionScopeModule());

        coreModules.add(new ServicesModule());

        coreModules.add(shiroModule());
        coreModules.add(shiroVaadinModule());
        coreModules.add(shiroAopModule());

        coreModules.add(servletModule());

        coreModules.add(standardPagesModule());

        coreModules.add(viewModule());

        coreModules.add(componentModule());

        coreModules.add(userModule());

        coreModules.add(optionModule());

        coreModules.add(eventBusModule());

        coreModules.add(navigationModule());

        coreModules.add(dataModule());
        coreModules.add(dataTypeModule());
        coreModules.add(pushModule());

        addUtilModules(coreModules);
        addValidationModules(coreModules);

        addAppModules(coreModules);
        addSitemapModules(coreModules);
        addPersistenceModules(coreModules);
        return coreModules;
    }

    protected void addUtilModules(List<Module> coreModules) {
        coreModules.add(new UtilsModule());
    }

    protected Module shiroAopModule() {
        return new KrailShiroAopModule();
    }


    /**
     * Override this if you have provided your own {@link DataTypeModule} implementation
     *
     * @return a new {@link DataTypeModule} instance
     */
    protected Module dataTypeModule() {
        return new DataTypeModule();
    }


    /**
     * Override this if you have provided your own {@link PushModule} implementation
     *
     * @return a new {@link PushModule} instance
     */
    protected Module pushModule() {
        return new PushModule();
    }

    protected Module uiModule() {
        return new TestUIModule();
    }

    /**
     * Override this if you have provided your own {@link DataModule} implementation
     *
     * @return a new {@link DataModule} instance
     */
    protected Module dataModule() {
        return new DataModule();
    }

    /**
     * Override this if you have provided your own {@link EventBusModule} implementation
     *
     * @return a new {@link EventBusModule} instance
     */
    protected Module eventBusModule() {
        return new EventBusModule();
    }

    /**
     * Override this if you have provided your own {@link NavigationModule}
     *
     * @return new instance of ApplicationConfigurationModule
     */

    protected AbstractModule navigationModule() {
        return new NavigationModule();
    }

    /**
     * Override this method if you want to use an alternative implementation for the Krail validation integration.  You
     * will need to keep the Apache Bval {{@link ValidationModule} unless you replace the the javax validation
     * implementation.
     *
     * @param modules the list used to collect modules for injector creation
     */
    protected void addValidationModules(List<Module> modules) {

        final Module validationModule = Modules.override(new ValidationModule())
                .with(new KrailValidationModule());
        modules.add(validationModule);
    }


    /**
     * Sets the default active source to read/write Option values from / to the in memory store
     * <p>
     * Override this if you have provided your own {@link OptionModule} or want to change the active source
     *
     * @return module instance
     */
    protected Module optionModule() {
        return new OptionModule().activeSource(InMemory.class);
    }

    /**
     * Override this if you have provided your own {@link I18NModule}
     *
     * @return a Module fr I18N
     */
    protected Module i18NModule() {
        return new TestI18NModule();
    }

    /**
     * Override this if you have provided your own {@link ApplicationConfigurationModule}
     *
     * @return new instance of ApplicationConfigurationModule
     */

    protected Module applicationConfigurationModule() {
        return new ApplicationConfigurationModule();
    }

    /**
     * Modules used in the creation of the {@link MasterSitemap} do not actually need to be separated, this just makes a convenient way of seeing them as a
     * group
     *
     * @param modules the list used to collect modules for injector creation
     */
    @SuppressFBWarnings("ACEM_ABSTRACT_CLASS_EMPTY_METHODS")
    protected void addSitemapModules(List<Module> modules) {
    }

    protected Module componentModule() {
        return new DefaultComponentModule();
    }

    /**
     * Override this if you have provided your own {@link ServletModule}
     *
     * @return servlet module instance
     */
    protected Module servletModule() {
        return new BaseServletModule();
    }

    /**
     * Override this method if you have sub-classed {@link ShiroVaadinModule} to provide your own bindings for Shiro
     * related exceptions.
     *
     * @return a module for bindings which realte to Shiro wihtin a Vaadin environment
     */
    protected Module shiroVaadinModule() {
        return new ShiroVaadinModule();
    }

    /**
     * Override this if you have sub-classed {@link StandardPagesModule} to provide bindings to your own standard page
     * views
     */
    protected Module standardPagesModule() {
        return new StandardPagesModule();
    }

    /**
     * Override this if you have sub-classed {@link ViewModule} to provide bindings to your own standard page views
     */
    protected Module viewModule() {
        return new ViewModule();
    }

    /**
     * Override this method if you have sub-classed {@link DefaultShiroModule} to provide bindings to your Shiro
     * related implementations (for example, {@link Realm} and {@link CredentialsMatcher}
     *
     * @return a new {@link DefaultShiroModule} instance
     */

    protected Module shiroModule() {
        return new DefaultShiroModule();
    }

    /**
     * Override this if you have sub-classed {@link UserModule} to provide bindings to your user related
     * implementations
     *
     * @return a new instance of {@link UserModule} or sub-class
     */
    protected UserModule userModule() {
        return new UserModule();
    }

    /**
     * Add as many application specific Guice modules as you wish by overriding this method.
     *
     * @param modules the list used to collect modules for injector creation
     */
    protected void addAppModules(List<Module> modules) {

    }

    /**
     * Add as many persistence related modules as needed.  These modules do not need to be separated, this just forms a convneient grouping for clarity
     *
     * @param modules the list used to collect modules for injector creation
     */
    protected void addPersistenceModules(List<Module> modules) {
        modules.add(new InMemoryModule().provideOptionDao()
                .providePatternDao());
    }
}
