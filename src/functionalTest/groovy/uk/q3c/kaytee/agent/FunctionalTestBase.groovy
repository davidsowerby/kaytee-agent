package uk.q3c.kaytee.agent

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonOutput
import org.apache.commons.io.FileUtils
import ratpack.groovy.test.embed.GroovyEmbeddedApp
import ratpack.guice.BindingsImposition
import ratpack.http.client.ReceivedResponse
import ratpack.http.client.RequestSpec
import ratpack.impose.ForceServerListenPortImposition
import ratpack.impose.ImpositionsSpec
import ratpack.jackson.Jackson
import ratpack.test.MainClassApplicationUnderTest
import ratpack.test.embed.EmbeddedApp
import ratpack.test.http.TestHttpClient
import spock.lang.AutoCleanup
import spock.lang.Specification
import uk.q3c.kaytee.agent.api.BuildRequest
import uk.q3c.kaytee.agent.app.ConstantsKt
import uk.q3c.kaytee.agent.app.Main
import uk.q3c.kaytee.agent.app.SubscriptionRequest
import uk.q3c.kaytee.agent.build.BuildRecord
import uk.q3c.kaytee.agent.system.DefaultInstallationInfo
import uk.q3c.kaytee.agent.system.InstallationInfo
import uk.q3c.rest.hal.HalMapper

import java.time.LocalDateTime

/**
 * Base class for functional
 *
 * Created by David Sowerby on 19 Feb 2017
 */
abstract class FunctionalTestBase extends Specification {
    EmbeddedApp subscriber = GroovyEmbeddedApp.ratpack {
        bindings {
            add(ObjectMapper.class, new HalMapper())
        }
        handlers {
            all {
                context.parse(Jackson.fromJson(BuildRecord.class)).then { buildRecord ->
                    if (subscriberMessages.isEmpty()) {
                        // first build request must be the one we want
                        buildId = buildRecord.uid
                    }
                    if (buildRecord.uid == buildId) {
                        if (buildRecord.hasCompleted()) {
                            buildComplete = true
                            finalRecord = buildRecord
                        }
                    }
                    subscriberMessages.add(buildRecord)
                    timeoutAt = LocalDateTime.now().plusSeconds(timeoutPeriod)
                    context.response.status(200)
                    context.render("OK")
                }
            }
        }
    }

    @AutoCleanup
    MainClassApplicationUnderTest aut

    File dataArea

    // Set this to the URI stub to be interacted with, for example 'buildRequests'
    String uri

    HalMapper halMapper

    @Delegate
    TestHttpClient client

    InstallationInfo installationInfo

    static timeoutPeriod = 20 // seconds per task
    static List<BuildRecord> subscriberMessages
    static UUID buildId
    static boolean buildComplete = false
    static LocalDateTime timeoutAt
    static BuildRecord finalRecord
    String subscriberUri
    boolean timedOut

    def setup() {
        File userHome = new File(System.getProperty("user.home"))
        dataArea = new File(userHome, "kaytee-data")
        System.setProperty(ConstantsKt.developmentMode_propertyName, "true")
        System.setProperty(ConstantsKt.baseDir_propertyName, dataArea.getAbsolutePath())
        installationInfo = new DefaultInstallationInfo()
        aut = createAut()
        client = aut.httpClient
        halMapper = new HalMapper()
        aut.properties

        subscriberMessages = new ArrayList<>()
        buildId = null
        buildComplete = false
        finalRecord = null
        timedOut = false
        subscriberUri = subscriber.address.toString()
    }

    def cleanup() {
        StringBuilder buf = new StringBuilder()
        for (msg in subscriberMessages) {
            buf.append(msg.summary())
        }
        FileUtils.writeStringToFile(new File("messages.txt"), buf.toString())
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

    protected void defaultSubscribe() {
        subscribe("http://localhost:9001/buildRecords", subscriberUri)
    }

    @SuppressWarnings("GroovyAssignabilityCheck")
    protected void submitRequest(BuildRequest buildRequest) {
        requestSpec { RequestSpec requestSpec ->
            requestSpec.body.type("application/hal+json")
            requestSpec.body.text(JsonOutput.toJson(buildRequest))
        }
    }

    @SuppressWarnings("GroovyAssignabilityCheck")
    protected ReceivedResponse subscribe(String toTopic, String subscriberCallback) {
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(new URL(toTopic), new URL(subscriberCallback))
        requestSpec { RequestSpec requestSpec ->
            requestSpec.body.type("application/hal+json")
            requestSpec.body.text(JsonOutput.toJson(subscriptionRequest))
        }
        ReceivedResponse response = post(ConstantsKt.subscriptions)
        return response
    }

}
