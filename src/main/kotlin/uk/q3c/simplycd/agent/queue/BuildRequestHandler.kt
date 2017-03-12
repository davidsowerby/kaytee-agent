package uk.q3c.simplycd.agent.queue

import com.google.inject.Singleton
import org.slf4j.LoggerFactory
import ratpack.handling.Context
import ratpack.handling.Handler
import ratpack.http.HttpMethod
import ratpack.jackson.Jackson
import ratpack.jackson.Jackson.fromJson
import ratpack.jackson.Jackson.json
import uk.q3c.build.gitplus.GitSHA
import uk.q3c.rest.hal.HalMapper
import uk.q3c.rest.hal.HalResource
import uk.q3c.simplycd.agent.api.BuildRequestRequest
import uk.q3c.simplycd.agent.app.ErrorResponseBuilder
import uk.q3c.simplycd.agent.app.buildRequests
import uk.q3c.simplycd.agent.i18n.DeveloperErrorMessageKey
import uk.q3c.simplycd.agent.project.Projects
import javax.inject.Inject

/**
 * Created by David Sowerby on 07 Mar 2017
 */
@Singleton
class BuildRequestHandler @Inject constructor(
        val requestQueue: RequestQueue,
        val errorResponseBuilder: ErrorResponseBuilder,
        val projects: Projects,
        val halMapper: HalMapper)

    : Handler {

    private val log = LoggerFactory.getLogger(this.javaClass.name)
    override fun handle(context: Context) {
        when (context.request.method) {
            HttpMethod.GET -> {
                log.debug("GET received")
// get the param
                val responseObject = HalResource()
                responseObject.self(buildRequests)
                context.response.status(200)
                context.render(Jackson.json(responseObject))
            }
            HttpMethod.POST -> {
                log.debug("POST received")
                context.parse(fromJson(BuildRequestRequest::class.java))
                        .then { buildRequestRequest ->
                            log.debug("processing build request")
                            val project = projects.getProject(buildRequestRequest)
                            val uid = requestQueue.addRequest(project, GitSHA(buildRequestRequest.commitId))
                            val response = BuildRequestResponse(buildRequestRequest, uid)
                            context.response.status(201)
                            context.render(json(response))
                        }
//                val buildRequestResource  = halMapper.readValue(context.request.getBody().getText(), BuildRequestResource::class.java)
//                buildRequestResource.
//                requestQueue.addRequest(project = project, gitSHA = gitSha)
                //validate the request, add to queue
                // return build request id

            }
            HttpMethod.DELETE -> {
// get the  param remove the request
            }

            else -> {
                val errorResponse = errorResponseBuilder.build(DeveloperErrorMessageKey.InvalidMethod, context.request.method, buildRequests, "GET, POST, DELETE")
                context.render(Jackson.json(errorResponse))
            }
        }

    }
}