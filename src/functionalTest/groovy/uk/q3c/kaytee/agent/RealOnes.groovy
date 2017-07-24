package uk.q3c.kaytee.agent

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonOutput
import org.apache.commons.io.FileUtils
import ratpack.groovy.test.embed.GroovyEmbeddedApp
import ratpack.http.Status
import ratpack.http.client.ReceivedResponse
import ratpack.http.client.RequestSpec
import ratpack.jackson.Jackson
import ratpack.test.embed.EmbeddedApp
import uk.q3c.kaytee.agent.api.BuildRequest
import uk.q3c.kaytee.agent.app.ConstantsKt
import uk.q3c.kaytee.agent.app.SubscriptionRequest
import uk.q3c.kaytee.agent.build.BuildRecord
import uk.q3c.kaytee.agent.i18n.BuildStateKey
import uk.q3c.rest.hal.HalMapper

import java.time.Duration
import java.time.LocalDateTime

/**
 * Created by David Sowerby on 21 Mar 2017
 */
@SuppressWarnings("GroovyAssignabilityCheck")
class RealOnes extends FunctionalTestBase {

    static timeoutPeriod = 20 // seconds per task
    static List<BuildRecord> subscriberMessages
    static UUID buildId
    static boolean buildComplete = false
    static LocalDateTime timeoutAt
    static BuildRecord finalRecord

    File tempDataArea

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
                }
//TODO response    WARN  r.s.internal.NettyHandlerAdapter - No response sent for PUT request to / (last handler: closure at line 40 of FunctionalTest1.groovy)
            }
        }
    }

    String subscriberUri
    boolean timedOut
//    HttpClient httpClient

    def setup() {
        tempDataArea = new File(temp, "kaytee-data")
        subscriberMessages = new ArrayList<>()
        buildId = null
        buildComplete = false
        finalRecord = null
        timedOut = false
        subscriberUri = subscriber.address.toString()
        System.setProperty(ConstantsKt.baseDir_propertyName, "/home/david/kaytee-data")


    }

    def cleanup() {
        StringBuilder buf = new StringBuilder()
        for (msg in subscriberMessages) {
            buf.append(msg.summary())
        }
        FileUtils.writeStringToFile(new File("messages.txt"), buf.toString())
    }

    def "subscribe to build records"() {
        when:
        subscribe("http://localhost:9001/buildRecords", subscriberUri)

        then:
        response.status == Status.OK
    }

    /**
     * Tests for all conditions of build stopping
     * @return
     */

    private boolean buildStopped() {
        if (LocalDateTime.now().isAfter(timeoutAt)) {
            timedOut = true
            return true
        }
        return buildComplete
    }


    def "selectable"() {
        given:
        timeoutPeriod = 18000 // 5 mins
        defaultSubscribe()
        BuildRequest buildRequest = new BuildRequest(projectName, commitId)
        timeoutAt = LocalDateTime.now().plusSeconds(timeoutPeriod)


        when:
        requestSpec { RequestSpec requestSpec ->
            requestSpec.body.type("application/hal+json")
            requestSpec.body.text(JsonOutput.toJson(buildRequest))
        }
        ReceivedResponse response = post(ConstantsKt.buildRequests)
        String t = response.getBody()
        println t
        while (!buildStopped()) {
            int togo = Duration.between(LocalDateTime.now(), timeoutAt).seconds
            println "Waiting for build to complete, timeout in $togo seconds    "
            Thread.sleep(1000)
        }
        then:
        !timedOut
        finalRecord.state == BuildStateKey.Complete
        finalRecord.outcome == BuildStateKey.Successful

        where:

        projectName                 | commitId
        "davidsowerby/q3c-testUtil" | "25b4a86a18386f5d10ea8829451c3155aa4fba14"
//        "davidsowerby/gitplus"      | "222ca19a8ddc61db0c54962fcea700648efd5f95"
//        "davidsowerby/changelog"      | "1ec1c8709dbf3502692273a7a5ddd767bf7d5912"
//        "davidsowerby/kaytee-plugin"      | "ed0c718c17d35167ce5e6d4d59ce50aa3f67824a"
    }

    private void defaultSubscribe() {
        subscribe("http://localhost:9001/buildRecords", subscriberUri)
    }

    private ReceivedResponse subscribe(String toTopic, String subscriberCallback) {
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(new URL(toTopic), new URL(subscriberCallback))
        requestSpec { RequestSpec requestSpec ->
            requestSpec.body.type("application/hal+json")
            requestSpec.body.text(JsonOutput.toJson(subscriptionRequest))
        }
        ReceivedResponse response = post(ConstantsKt.subscriptions)
        return response

    }
}
