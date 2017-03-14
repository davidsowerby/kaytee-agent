package uk.q3c.simplycd.agent.app

import org.slf4j.LoggerFactory
import ratpack.handling.Context
import ratpack.handling.Handler
import ratpack.http.HttpMethod
import ratpack.jackson.Jackson
import uk.q3c.simplycd.agent.api.BuildRequestRequest
import uk.q3c.simplycd.agent.i18n.DeveloperErrorMessageKey
import javax.inject.Inject

/**
 * Created by David Sowerby on 13 Mar 2017
 */
class HookHandler @Inject constructor(val errorResponseBuilder: ErrorResponseBuilder) : Handler {
    private val log = LoggerFactory.getLogger(this.javaClass.name)

    override fun handle(context: Context) {
        when (context.request.method) {
            HttpMethod.POST -> {
                log.debug("POST received")
                context.parse(Jackson.fromJson(BuildRequestRequest::class.java))
                        .then { buildRequestRequest ->
                            log.debug("processing hook request")
                            try {
//                                val project = projects.getProject(buildRequestRequest)
//                                val uid = requestQueue.addRequest(project, GitSHA(buildRequestRequest.commitId))
//                                val response = BuildRequestResponse(buildRequestRequest, uid)
                                context.response.status(200)
//                                context.render(Jackson.json(response))
                            } catch (e: Exception) {
                                val errorResponse = errorResponseBuilder.build(DeveloperErrorMessageKey.Invalid_Project_Name, e.message ?: "no message")
                                context.response.status(DeveloperErrorMessageKey.Invalid_Project_Name.httpCode)
                                context.render(Jackson.json(errorResponse))
                            }

                        }
            }

            else -> {
                val errorResponse = errorResponseBuilder.build(DeveloperErrorMessageKey.Invalid_Method, context.request.method, buildRequests, "GET, POST, DELETE")
                context.render(Jackson.json(errorResponse))
            }
        }
    }
}