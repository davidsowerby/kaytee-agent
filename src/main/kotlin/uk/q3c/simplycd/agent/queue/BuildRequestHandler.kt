package uk.q3c.simplycd.agent.queue

import com.google.common.collect.ImmutableList
import com.google.inject.Singleton
import org.slf4j.LoggerFactory
import ratpack.handling.Context
import ratpack.http.HttpMethod
import ratpack.jackson.Jackson
import uk.q3c.build.gitplus.GitSHA
import uk.q3c.rest.hal.HalMapper
import uk.q3c.simplycd.agent.api.BuildRequest
import uk.q3c.simplycd.agent.api.BuildRequestResponse
import uk.q3c.simplycd.agent.app.AbstractHandler
import uk.q3c.simplycd.agent.app.ErrorResponseBuilder
import uk.q3c.simplycd.agent.i18n.DeveloperErrorMessageKey
import uk.q3c.simplycd.agent.project.Projects
import javax.inject.Inject

/**
 * Created by David Sowerby on 07 Mar 2017
 */
@Singleton
class BuildRequestHandler @Inject constructor(
        val requestQueue: RequestQueue,
        errorResponseBuilder: ErrorResponseBuilder,
        val projects: Projects,
        val halMapper: HalMapper)

    : AbstractHandler(errorResponseBuilder) {

    init {
        validMethodCalls = ImmutableList.of(HttpMethod.POST)
    }

    private val log = LoggerFactory.getLogger(this.javaClass.name)
    override fun post(context: Context) {
        context.parse(Jackson.fromJson(BuildRequest::class.java))
                .then { buildRequest ->
                    log.debug("processing build request for project: '{}'", buildRequest.projectFullName)
                    try {
                        val project = projects.getProject(buildRequest)
                        val uid = requestQueue.addRequest(project, GitSHA(buildRequest.commitId))
                        val response = BuildRequestResponse(buildRequest, uid)
                        context.response.status(202)
                        context.render(Jackson.json(response))
                    } catch (e: Exception) {
                        val errorResponse = errorResponseBuilder.build(DeveloperErrorMessageKey.Invalid_Project_Name, e.message ?: "no message")
                        context.response.status(DeveloperErrorMessageKey.Invalid_Project_Name.httpCode)
                        context.render(Jackson.json(errorResponse))
                    }

                }
    }
}