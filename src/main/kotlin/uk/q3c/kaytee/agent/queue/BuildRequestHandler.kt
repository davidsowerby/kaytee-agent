package uk.q3c.kaytee.agent.queue

import com.google.common.collect.ImmutableList
import com.google.inject.Singleton
import org.slf4j.LoggerFactory
import ratpack.handling.Context
import ratpack.http.HttpMethod
import ratpack.jackson.Jackson
import uk.q3c.build.gitplus.GitSHA
import uk.q3c.kaytee.agent.api.BuildRequest
import uk.q3c.kaytee.agent.api.BuildRequestResponse
import uk.q3c.kaytee.agent.app.AbstractHandler
import uk.q3c.kaytee.agent.app.ErrorResponseBuilder
import uk.q3c.kaytee.agent.app.buildRequests
import uk.q3c.kaytee.agent.i18n.DeveloperErrorMessageKey
import uk.q3c.kaytee.agent.project.Projects
import javax.inject.Inject

/**
 * Created by David Sowerby on 07 Mar 2017
 */
@Singleton
class BuildRequestHandler @Inject constructor(
        val requestQueue: RequestQueue,
        errorResponseBuilder: ErrorResponseBuilder,
        val projects: Projects)

    : AbstractHandler(errorResponseBuilder) {

    init {
        validMethodCalls = ImmutableList.of(HttpMethod.POST)
        uri = buildRequests
    }

    private val log = LoggerFactory.getLogger(this.javaClass.name)
    override fun post(context: Context) {
        context.parse(Jackson.fromJson(BuildRequest::class.java))
                .then { buildRequest ->
                    log.debug("processing build request for project: '{}'", buildRequest.projectUri)
                    try {
                        val project = projects.getProject(buildRequest)
                        val uid = requestQueue.addRequest(project, GitSHA(buildRequest.commitId))
                        val response = BuildRequestResponse(buildRequest, uid)
                        context.response.status(202)
                        context.render(Jackson.json(response))
                    } catch (e: Exception) {
                        val errorResponse = errorResponseBuilder.build(uri, DeveloperErrorMessageKey.Invalid_Project_Name, e.message ?: "no message")
                        context.response.status(DeveloperErrorMessageKey.Invalid_Project_Name.httpCode)
                        context.render(Jackson.json(errorResponse))
                    }

                }
    }
}