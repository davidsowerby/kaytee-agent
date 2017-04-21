package uk.q3c.simplycd.agent

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import ratpack.guice.BindingsImposition
import ratpack.impose.ForceServerListenPortImposition
import ratpack.impose.ImpositionsSpec
import ratpack.test.MainClassApplicationUnderTest
import ratpack.test.http.TestHttpClient
import spock.lang.AutoCleanup
import spock.lang.Specification
import uk.q3c.rest.hal.HalMapper
import uk.q3c.simplycd.agent.app.ConstantsKt
import uk.q3c.simplycd.agent.app.Main
import uk.q3c.simplycd.agent.system.DefaultInstallationInfo
import uk.q3c.simplycd.agent.system.InstallationInfo
/**
 * Base class for functional
 *
 * Created by David Sowerby on 19 Feb 2017
 */
abstract class FunctionalTestBase extends Specification {

    @AutoCleanup
    MainClassApplicationUnderTest aut

    @Rule
    TemporaryFolder temporaryFolder
    File temp

    // Set this to the URI stub to be interacted with, for example 'buildRequests'
    String uri

    @Delegate
    TestHttpClient client

    HalMapper halMapper

    InstallationInfo installationInfo

    def setup() {
        temp = temporaryFolder.getRoot()
        System.setProperty(ConstantsKt.developmentMode_propertyName, "true")
        System.setProperty(ConstantsKt.baseDir_propertyName, temp.getAbsolutePath())
        installationInfo = new DefaultInstallationInfo()
        aut = createAut()
        client = aut.httpClient
        halMapper = new HalMapper()
        aut.properties
    }

    protected MainClassApplicationUnderTest createAut() {
        return new MainClassApplicationUnderTest(Main.class) {

            @Override
            protected void addImpositions(ImpositionsSpec impositions) {
                impositions.add(ForceServerListenPortImposition.of(9001))
                impositions.add(
                        BindingsImposition.of {
                            it.bindInstance(InstallationInfo.class, installationInfo)
                        })
            }
        }
    }

}
