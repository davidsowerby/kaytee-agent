package uk.q3c.simplycd.agent

import groovy.json.JsonOutput
import ratpack.http.client.RequestSpec
import uk.q3c.simplycd.agent.api.BuildRequest
import uk.q3c.simplycd.agent.app.ConstantsKt

/**
 * Created by David Sowerby on 21 Mar 2017
 */
class FunctionalTest1 extends FunctionalTestBase {


    @SuppressWarnings("GroovyAssignabilityCheck")
    "run a known good build"() {
        given:
        final String fullProjectName = "davidsowerby/simplycd-test"
        final String commitId = "7c3a779e17d65ec255b4c7d40b14950ea6ce232e"
        BuildRequest buildRequest = new BuildRequest(fullProjectName, commitId)


        when:
        requestSpec { RequestSpec requestSpec ->
            requestSpec.body.type("application/hal+json")
            requestSpec.body.text(JsonOutput.toJson(buildRequest))
        }
        post(ConstantsKt.buildRequests)
        Thread.sleep(10000)

        then:
        true
    }
}
