package uk.q3c.simplycd.agent.queue

import groovy.json.JsonOutput
import ratpack.guice.BindingsImposition
import ratpack.http.client.RequestSpec
import ratpack.impose.ImpositionsSpec
import ratpack.test.MainClassApplicationUnderTest
import uk.q3c.simplycd.agent.app.ConstantsKt
import uk.q3c.simplycd.agent.app.HandlerTest
import uk.q3c.simplycd.agent.app.Main

/**
 * Created by David Sowerby on 10 Mar 2017
 */
class BuildRequestHandlerTest extends HandlerTest {

    BuildRequestRequest buildRequest
    RequestQueue mockRequestQueue = Mock(RequestQueue)

    def setup() {
        uri = ConstantsKt.buildRequests
    }

    @Override
    protected MainClassApplicationUnderTest createAut() {
        return new MainClassApplicationUnderTest(Main.class) {
            @Override
            protected void addImpositions(ImpositionsSpec impositions) {
                impositions.add(
                        BindingsImposition.of {
                            it.bindInstance(RequestQueue.class, mockRequestQueue)
                        })
            }
        }
    }

    def "post valid build request"() {
        given:
        final UUID uuid = UUID.randomUUID()
        final String projectName = "davidsowerby/q3c-testUtil"
        final String commitId = "9173501a05e33ca549cb83f5d8015d45bf5c4510"
        buildRequest = new BuildRequestRequest(projectName, commitId)

        when:
        requestSpec { RequestSpec requestSpec ->
            requestSpec.body.type("application/hal+json")
            requestSpec.body.text(JsonOutput.toJson(buildRequest))
        }
        post(uri)

        then:
        1 * mockRequestQueue.addRequest(_, _) >> uuid
        response.statusCode == 201
        BuildRequestResponse buildRequestResponse = halMapper.readValue(response.body.text, BuildRequestResponse)
        with(buildRequestResponse) {
            projectFullName == projectName
            commitId == commitId
            buildId != null
            buildId != UUID.randomUUID()
        }
    }
}
