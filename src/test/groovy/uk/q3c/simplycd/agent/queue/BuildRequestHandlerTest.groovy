package uk.q3c.simplycd.agent.queue

import groovy.json.JsonOutput
import org.apache.http.HttpStatus
import ratpack.guice.BindingsImposition
import ratpack.http.client.RequestSpec
import ratpack.impose.ImpositionsSpec
import ratpack.test.MainClassApplicationUnderTest
import uk.q3c.simplycd.agent.app.ConstantsKt
import uk.q3c.simplycd.agent.app.ErrorResponse
import uk.q3c.simplycd.agent.app.HandlerTest
import uk.q3c.simplycd.agent.app.Main

/**
 * Created by David Sowerby on 10 Mar 2017
 */
@SuppressWarnings("GroovyAssignabilityCheck")
class BuildRequestHandlerTest extends HandlerTest {

    BuildRequestRequest buildRequest
    RequestQueue mockRequestQueue = Mock(RequestQueue)
    UUID uuid
    String projectName = "davidsowerby/q3c-testUtil"
    String commitId = "9173501a05e33ca549cb83f5d8015d45bf5c4510"

    def setup() {
        uri = ConstantsKt.buildRequests
        projectName = "davidsowerby/q3c-testUtil"
        commitId = "9173501a05e33ca549cb83f5d8015d45bf5c4510"
        uuid = UUID.randomUUID()
        buildRequest = new BuildRequestRequest(projectName, commitId)
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

        when:
        requestSpec { RequestSpec requestSpec ->
            requestSpec.body.type("application/hal+json")
            requestSpec.body.text(JsonOutput.toJson(buildRequest))
        }
        post(uri)

        then:
        1 * mockRequestQueue.addRequest(_, _) >> uuid
        response.statusCode == HttpStatus.SC_ACCEPTED
        BuildRequestResponse buildRequestResponse = halMapper.readValue(response.body.text, BuildRequestResponse)
        with(buildRequestResponse) {
            projectFullName == projectName
            commitId == commitId
            buildId != null
            buildId == uuid
        }
    }

    def "get valid build request id"() {

        expect: false
    }

    def "get unrecognised build request"() {

        expect: false
    }

    @SuppressWarnings("GroovyAssignabilityCheck")
    "post build request with invalid name"() {
        given:
        buildRequest = new BuildRequestRequest("davidsowerbyq3ctestUtil", commitId)

        when:
        requestSpec { RequestSpec requestSpec ->
            requestSpec.body.type("application/hal+json")
            requestSpec.body.text(JsonOutput.toJson(buildRequest))
        }
        post(uri)

        then:
        0 * mockRequestQueue.addRequest(_, _) >> uuid
        response.statusCode == HttpStatus.SC_BAD_REQUEST
        ErrorResponse buildRequestResponse = halMapper.readValue(response.body.text, ErrorResponse)
        with(buildRequestResponse) {
            httpCode == HttpStatus.SC_BAD_REQUEST
            detailCode == "Invalid_Project_Name"
        }

    }

    def "hook notified when build request starts"() {

        expect: false
    }
}
