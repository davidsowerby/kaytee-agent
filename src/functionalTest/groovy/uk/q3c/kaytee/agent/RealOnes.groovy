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

        projectName                 | commitId
//        "davidsowerby/q3c-testutils" | "0f50942a7be0e8520d283683955356cc4eafe617"
//        "davidsowerby/gitplus"      | "780af5563a26a1027e259f1a684a5e8b6169da93"
//        "davidsowerby/changelog"      | "4f5dd6e1f97c94011b653b375dab6af53bcf8a19"
        "davidsowerby/kaytee-plugin" | "c2fbeb37b9cd17d05c121537730e4ef8f0c7e080"
//        "davidsowerby/projectadmin"      | "74653a2c6b5c56d3d86a36bbaaab9ba29be93651"
//        "davidsowerby/hal-kotlin"      | "a35241d1f0c4e619e337499e351a45cb1abd6db2"
//        "davidsowerby/kaytee-test"  | "7ac2e38d98118837fd65fea5f32e2ef8b49cca53"  // all pass
//        "davidsowerby/hal-kotlin" | "db324765d4bb7298336db94661ff89717ce25836"
//        "davidsowerby/krail-quartz" | "4cd389070ae3c4dce7bb322f449aa2e11c1e2977"
//        "davidsowerby/krail-jpa" | "a71978e7892fa3702e5504fb612f992f4524f17b"
//        "davidsowerby/krail-bench" | "d7a6bfc4afa230cdfb027a770bfce9a59d608e8c"
//        "davidsowerby/q3c-util" | "30bf1aef157e5c1f5f68d17b841179215e4899c1"
//        "davidsowerby/krail-i18n-api" | "dd444417d7a0d66a067b63e147cb6c00cdfb5aaf"
//        "davidsowerby/krail-i18n" | "c6252039bff5f70ed215dc1b55c2962fc551d3ee"
//        "davidsowerby/krail-persist-api" | "86731eb4c9e5ff61bba0d448c769190564ee97e2"
//        "davidsowerby/krail-option-api" | "779c3d41f3ae41950e38ea202b871870733f0052"
//        "davidsowerby/krail-option" | "9e825e237235acda5305826e8643dccf768aa468"
//        "davidsowerby/krail-service-api" | "27d7c613b635fb168f34476df6d4dea7d3ceff9d"
//        "davidsowerby/krail-config-api" | "429dffef7d010165cc45abc341ca486f6de00540"
//        "davidsowerby/krail-config" | "3ec39489221d62b4f9246b1897c5a7fd1c7e6b96"
//        "davidsowerby/eventbus-api" | "22cb561e7b173af85f570e7615b86de344c4cefa"
//        "davidsowerby/krail-testapp" | "0091cd9610e862eb3b89d3be0672156982413bdf"
//        "davidsowerby/krail" | "8026fe2a7dbe0bfe01b313c258bf93276d031b71"


    }
}