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

        projectName              | commitId
//        "davidsowerby/q3c-testutils" | "224418e162e563f2c36a63db87975ab8fe9fce95"
//        "davidsowerby/gitplus"      | "81b6c50d190f0e18f532700ce785714bf4d209c9"
//        "davidsowerby/changelog"      | "d67f3254677755ef7db236a0c1ede3b0fbdc728d"
//        "davidsowerby/kaytee-plugin" | "684cbfaceef6e428f9b61ddad8595b20651d553d"
//        "davidsowerby/projectadmin"      | "74653a2c6b5c56d3d86a36bbaaab9ba29be93651"
//        "davidsowerby/hal-kotlin"      | "a35241d1f0c4e619e337499e351a45cb1abd6db2"
//        "davidsowerby/kaytee-test"  | "7ac2e38d98118837fd65fea5f32e2ef8b49cca53"  // all pass
//        "davidsowerby/hal-kotlin" | "db324765d4bb7298336db94661ff89717ce25836"
//        "davidsowerby/krail-quartz" | "4cd389070ae3c4dce7bb322f449aa2e11c1e2977"
        "davidsowerby/krail-jpa" | "03f081d591b1911b295c1a1a2ed23e59aead5b6c"
//        "davidsowerby/krail-bench" | "be496da515f80babcae4f0661ffe341568ad92e4"
//        "davidsowerby/q3c-util" | "77e494d115045b94bb738488d833e21d66bf1c3f"
//        "davidsowerby/krail-i18n-api" | "78f15d64d9b19028b7e3c85502eb90ede561538d"
//        "davidsowerby/krail-i18n" | "74b01ba762ec0255680c9d140b991a5edaaf42ce"
//        "davidsowerby/krail-persist-api" | "d76905c850ea8577992fb84cf9c09aec7a01944c"
//        "davidsowerby/krail-option-api" | "5aa21238d27b72644f15f82a3db4a6cfb4bbf1b8"
//        "davidsowerby/krail-option" | "d285b0f192d8f0e0ea8e23c29551e69f7237f671"
//        "davidsowerby/krail-service-api" | "99b66cd7a7958e8ca1e161f768f8f544748e874c"
//        "davidsowerby/krail-config-api" | "b12ae7a632762941aab354d69cca889ee55348a9"
//        "davidsowerby/krail-config" | "fbc9a4d56c1f0b4c48798ea34d8dac0c180ba429"
//        "davidsowerby/eventbus-api" | "85a4883bed56b4adfee1865ff5ca844b1b52d14b"
//        "davidsowerby/eventbus-mbassador" | "4f0490ef8107eac400067035b6466b665bacb93f"
//        "davidsowerby/krail-testapp" | "333ec6a997a0a75da4a4e61a514ccb26a57e3739"
//        "davidsowerby/krail" | "6130024d9b45f4611aa948bd9855f62c6cc3ffe3"


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