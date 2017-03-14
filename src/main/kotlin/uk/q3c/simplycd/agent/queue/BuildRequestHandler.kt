package uk.q3c.simplycd.agent.queue

import com.google.inject.Singleton
import org.slf4j.LoggerFactory
import ratpack.handling.Context
import ratpack.handling.Handler
import ratpack.http.HttpMethod
import ratpack.jackson.Jackson.fromJson
import ratpack.jackson.Jackson.json
import uk.q3c.build.gitplus.GitSHA
import uk.q3c.rest.hal.HalMapper
import uk.q3c.simplycd.agent.api.BuildRequestRequest
import uk.q3c.simplycd.agent.app.ErrorResponseBuilder
import uk.q3c.simplycd.agent.app.buildRequests
import uk.q3c.simplycd.agent.i18n.DeveloperErrorMessageKey.Invalid_Method
import uk.q3c.simplycd.agent.i18n.DeveloperErrorMessageKey.Invalid_Project_Name
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
            HttpMethod.POST -> {
                log.debug("POST received")
                context.parse(fromJson(BuildRequestRequest::class.java))
                        .then { buildRequestRequest ->
                            log.debug("processing build request")
                            try {
                                val project = projects.getProject(buildRequestRequest)
                                val uid = requestQueue.addRequest(project, GitSHA(buildRequestRequest.commitId))
                                val response = BuildRequestResponse(buildRequestRequest, uid)
                                context.response.status(202)
                                context.render(json(response))
                            } catch (e: Exception) {
                                val errorResponse = errorResponseBuilder.build(Invalid_Project_Name, e.message ?: "no message")
                                context.response.status(Invalid_Project_Name.httpCode)
                                context.render(json(errorResponse))
                            }

                        }
            }

            else -> {
                val errorResponse = errorResponseBuilder.build(Invalid_Method, context.request.method, buildRequests, "GET, POST, DELETE")
                context.render(json(errorResponse))
            }
        }

    }
}