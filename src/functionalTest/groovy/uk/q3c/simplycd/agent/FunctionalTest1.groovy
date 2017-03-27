package uk.q3c.simplycd.agent

import groovy.json.JsonOutput
import ratpack.http.client.RequestSpec
import uk.q3c.simplycd.agent.app.ConstantsKt
import uk.q3c.simplycd.agent.queue.BuildRequestRequest

/**
 * Created by David Sowerby on 21 Mar 2017
 */
class FunctionalTest1 extends FunctionalTestBase {


    @SuppressWarnings("GroovyAssignabilityCheck")
    "run a known good build"() {
        given:
        final String fullProjectName = "davidsowerby/q3c-testUtil"
        final String commitId = "91a5818aced677660f0e25a7c57aa73601d8deb8"
        BuildRequestRequest buildRequest = new BuildRequestRequest(fullProjectName, commitId)


        when:
        requestSpec { RequestSpec requestSpec ->
            requestSpec.body.type("application/hal+json")
            requestSpec.body.text(JsonOutput.toJson(buildRequest))
        }
        post(ConstantsKt.buildRequests)

        then:
        true
    }
}
