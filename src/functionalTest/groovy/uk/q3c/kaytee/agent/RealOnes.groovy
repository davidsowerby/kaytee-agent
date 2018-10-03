package uk.q3c.kaytee.agent

import ratpack.http.Status
import ratpack.http.client.ReceivedResponse
import spock.lang.Ignore
import uk.q3c.build.gitplus.remote.ServiceProvider
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
        BuildRequest buildRequest = new BuildRequest(projectUrl, commitId, provider)
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

        projectUrl                               | commitId                                   | provider
//        "https://gitlab.com/dsowerby/q3c-testutils"        | "224418e162e563f2c36a63db87975ab8fe9fce95" | ServiceProvider.GITLAB
        "https://gitlab.com/krail-build/gitplus" | "211e7dacd5e0a2ad39f7ac74998ce83ff95be71a" | ServiceProvider.GITLAB
//        "https://gitlab.com/krail-build/changelog"         | "0e029139529d58310b267a0883079b5c94fb95af" | ServiceProvider.GITLAB
//        "https://gitlab.com/krail-build/kaytee-plugin"     | "55eef2566fa630a5e42aa739028e6d55c4e2ed54" | ServiceProvider.GITLAB
//        "https://gitlab.com/krail-build/projectadmin"      | "74653a2c6b5c56d3d86a36bbaaab9ba29be93651" | ServiceProvider.GITLAB
//        "https://github.com/davidsowerby/hal-kotlin"       | "a35241d1f0c4e619e337499e351a45cb1abd6db2" | ServiceProvider.GITHUB
//        "https://github.com/davidsowerby/kaytee-test"      | "7ac2e38d98118837fd65fea5f32e2ef8b49cca53" | ServiceProvider.GITHUB // all pass
//        "https://github.com/davidsowerby/hal-kotlin"       | "db324765d4bb7298336db94661ff89717ce25836" | ServiceProvider.GITHUB
//        "https://github.com/davidsowerby/krail-quartz"     | "4cd389070ae3c4dce7bb322f449aa2e11c1e2977" | ServiceProvider.GITHUB
//        "https://github.com/davidsowerby/krail-jpa"        | "28849807307062c844375b6ab8af028cc75e9799" | ServiceProvider.GITHUB
//        "https://github.com/davidsowerby/krail-bench"      | "be496da515f80babcae4f0661ffe341568ad92e4" | ServiceProvider.GITHUB
//        "https://github.com/KrailOrg/q3c-util"             | "36b3b409c5702aeb19431239220305773dcccda6" | ServiceProvider.GITHUB
//        "https://github.com/KrailOrg/krail-i18n-api"       | "86726ac84a5a9b28c12a5d28f85074ab3eea5ea5" | ServiceProvider.GITHUB
//        "https://github.com/KrailOrg/krail-i18n"           | "f9d1536028eb298360173baf7f49bb19ac61d08b" | ServiceProvider.GITHUB
//        "https://github.com/KrailOrg/krail-persist-api"    | "d76905c850ea8577992fb84cf9c09aec7a01944c" | ServiceProvider.GITHUB
//        "https://github.com/KrailOrg/krail-option-api"     | "e6735cf7919c65c7b18e2cfb1a460c0e8557db66" | ServiceProvider.GITHUB
//        "https://github.com/KrailOrg/krail-option"         | "f736f1f151f95147ff8062fcb04790bf8991fbfa" | ServiceProvider.GITHUB
//        "https://github.com/KrailOrg/krail-service-api"    | "6447f41d1d4cac57d5d1cc85ebf729816f7f9a3b" | ServiceProvider.GITHUB
//        "https://github.com/davidsowerby/service-api"      | "9784167b5078da1b59a62c36776cce4dbeb1d62f" | ServiceProvider.GITHUB
//        "https://github.com/KrailOrg/krail-config-api"     | "881aa185a0b382dc499993870727498a5272491e" | ServiceProvider.GITHUB
//        "https://github.com/KrailOrg/krail-config"         | "324925edff86aedbaee3c3f20f37673d01b8a22f" | ServiceProvider.GITHUB
//        "https://github.com/KrailOrg/eventbus-api"         | "85a4883bed56b4adfee1865ff5ca844b1b52d14b" | ServiceProvider.GITHUB
//        "https://github.com/KrailOrg/eventbus-mbassador"   | "4f0490ef8107eac400067035b6466b665bacb93f" | ServiceProvider.GITHUB
//        "https://github.com/KrailOrg/krail-testapp"        | "e109fad929cdda4d53302656e384ddee92804e2e" | ServiceProvider.GITHUB
//        "https://github.com/KrailOrg/krail"                | "cf49e843f47da90e743de34168d3fcbd790eb1f9" | ServiceProvider.GITHUB
//        "https://gitlab.com/dsowerby/serialization-tracer" | "8277eec7ebe645e6c8e798bb37f5ff1de75fe954" | ServiceProvider.GITLAB

    }

    @Ignore
    def "vaadin7"() {
        given:
        timeoutPeriod = 18000 // 5 mins
        defaultSubscribe()
        BuildRequest buildRequest = new BuildRequest(projectUrl, commitId, provider)
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

        projectUrl                 | commitId
//        "davidsowerby/krail-jpa" | "1dc2d7a34f50753fffddd1a62858186daf25f49a"
//        "davidsowerby/krail"     | "df5849d291c3ca5b34c7208446086c41216e37bb"
        "davidsowerby/krail-bench" | "8424c57da4dcef4965b1ed333eae231df8cf5db2"
    }

    @Ignore
    def "compatibility version"() {
        given:
        timeoutPeriod = 18000 // 5 mins
        defaultSubscribe()
        BuildRequest buildRequest = new BuildRequest(projectUrl, commitId, provider)
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

        projectUrl                   | commitId
//        "davidsowerby/krail-jpa" | "cda3a5c319c4b5e3474326ff689cb8b6492e02fb"
//        "davidsowerby/krail"     | "8623e08e186d25e53f9fcaf25724aae5a531d872"
        "davidsowerby/krail-bench"   | "b8ef621bc81a563b650af91cc9933b7886fc675e"
        "davidsowerby/krail-testapp" | "4e8d61aa9ed654bbc57461feb8db53b12debdef0"
    }


}