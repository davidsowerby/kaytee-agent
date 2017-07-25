package uk.q3c.kaytee.agent

import ratpack.http.Status
import ratpack.http.client.ReceivedResponse
import uk.q3c.kaytee.agent.api.BuildRequest
import uk.q3c.kaytee.agent.app.ConstantsKt
import uk.q3c.kaytee.agent.i18n.BuildStateKey

import java.time.Duration
import java.time.LocalDateTime
/**
 * Created by David Sowerby on 21 Mar 2017
 */
@SuppressWarnings("GroovyAssignabilityCheck")
class RealOnes extends FunctionalTestBase {



    def setup() {

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
        submitRequest(buildRequest)
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
//        "davidsowerby/q3c-testUtil" | "25b4a86a18386f5d10ea8829451c3155aa4fba14"
//        "davidsowerby/gitplus"      | "222ca19a8ddc61db0c54962fcea700648efd5f95"
//        "davidsowerby/changelog"      | "1ec1c8709dbf3502692273a7a5ddd767bf7d5912"
//        "davidsowerby/kaytee-plugin"      | "ed0c718c17d35167ce5e6d4d59ce50aa3f67824a"
//        "davidsowerby/projectadmin"      | "74653a2c6b5c56d3d86a36bbaaab9ba29be93651"
//        "davidsowerby/hal-kotlin"      | "a35241d1f0c4e619e337499e351a45cb1abd6db2"
        "davidsowerby/kaytee-test"  | "7ac2e38d98118837fd65fea5f32e2ef8b49cca53"  // all pass
    }


}
