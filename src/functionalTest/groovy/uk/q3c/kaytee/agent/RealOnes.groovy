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
            print "Waiting for build to complete, timeout in $togo seconds    \r"
            Thread.sleep(1000)
        }
        then:
        !timedOut
        finalRecord.state == BuildStateKey.Complete
        finalRecord.outcome == BuildStateKey.Successful

        where:

        projectName                      | commitId
//        "davidsowerby/q3c-testutils" | "224418e162e563f2c36a63db87975ab8fe9fce95"
//        "davidsowerby/gitplus"      | "67e3f5372b8365687ac97c5b057c4049dfe405b0"
//        "davidsowerby/changelog"      | "8dede082161afc3507b8c7c6c0419e18576ec7cc"
//        "davidsowerby/kaytee-plugin" | "9a990b070cd1b33b2e104834dbe10a04d6baeca1"
//        "davidsowerby/projectadmin"      | "74653a2c6b5c56d3d86a36bbaaab9ba29be93651"
//        "davidsowerby/hal-kotlin"      | "a35241d1f0c4e619e337499e351a45cb1abd6db2"
//        "davidsowerby/kaytee-test"  | "7ac2e38d98118837fd65fea5f32e2ef8b49cca53"  // all pass
//        "davidsowerby/hal-kotlin" | "db324765d4bb7298336db94661ff89717ce25836"
//        "davidsowerby/krail-quartz" | "4cd389070ae3c4dce7bb322f449aa2e11c1e2977"
//        "davidsowerby/krail-jpa" | "a71978e7892fa3702e5504fb612f992f4524f17b"
//        "davidsowerby/krail-bench" | "be496da515f80babcae4f0661ffe341568ad92e4"
//        "davidsowerby/q3c-util" | "ef36d712d07cec52da21407235d143b1340d7a5a"
//        "davidsowerby/krail-i18n-api" | "c2c1f03ec9253a84f627e603af2cde66e97d1107"
//        "davidsowerby/krail-i18n" | "f4da39d534710a894808e6dd2dc5f513ac5da7e7"
        "davidsowerby/krail-persist-api" | "2a4ecc3038a348824ebe01fd9d7f1bcda741275e"
//        "davidsowerby/krail-option-api" | "6e1bd25944b042d4519bd487fa07249bbdd4bd9b"
//        "davidsowerby/krail-option" | "6e1bd25944b042d4519bd487fa07249bbdd4bd9b"
//        "davidsowerby/krail-service-api" | "7c2738c897dbc562512643249779e90da550d2d7"
//        "davidsowerby/krail-config-api" | "c764a8ae7d05fe7c731ff49970662168513412d4"
//        "davidsowerby/krail-config" | "9eb074317a9f81674886ff6ed78f01fef84bbce0"
//        "davidsowerby/eventbus-api" | "82cc2046e41394af8030260eef4acc9d3431c72d"
//        "davidsowerby/eventbus-mbassador" | "de34a9430a8d7b8e01ae17df3b104aa18fe41d14"
//        "davidsowerby/krail-testapp" | "333ec6a997a0a75da4a4e61a514ccb26a57e3739"
//        "davidsowerby/krail" | "8026fe2a7dbe0bfe01b313c258bf93276d031b71"


    }


    def "vaadin7"() {
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
            print "Waiting for build to complete, timeout in $togo seconds    \r"
            Thread.sleep(1000)
        }
        then:
        !timedOut
        finalRecord.state == BuildStateKey.Complete
        finalRecord.outcome == BuildStateKey.Successful

        where:

        projectName                | commitId
//        "davidsowerby/krail-jpa" | "1dc2d7a34f50753fffddd1a62858186daf25f49a"
//        "davidsowerby/krail"     | "df5849d291c3ca5b34c7208446086c41216e37bb"
        "davidsowerby/krail-bench" | "8424c57da4dcef4965b1ed333eae231df8cf5db2"
    }


    def "compatibility version"() {
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
            print "Waiting for build to complete, timeout in $togo seconds    \r"
            Thread.sleep(1000)
        }
        then:
        !timedOut
        finalRecord.state == BuildStateKey.Complete
        finalRecord.outcome == BuildStateKey.Successful

        where:

        projectName                  | commitId
//        "davidsowerby/krail-jpa" | "cda3a5c319c4b5e3474326ff689cb8b6492e02fb"
//        "davidsowerby/krail"     | "8623e08e186d25e53f9fcaf25724aae5a531d872"
        "davidsowerby/krail-bench"   | "b8ef621bc81a563b650af91cc9933b7886fc675e"
        "davidsowerby/krail-testapp" | "4e8d61aa9ed654bbc57461feb8db53b12debdef0"
    }


}