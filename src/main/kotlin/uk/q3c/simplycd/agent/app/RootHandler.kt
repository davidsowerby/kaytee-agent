package uk.q3c.simplycd.agent.app

import ratpack.handling.Context
import ratpack.handling.Handler
import ratpack.http.HttpMethod
import ratpack.jackson.Jackson.json
import uk.q3c.rest.hal.HalLink
import uk.q3c.rest.hal.HalResource
import uk.q3c.simplycd.agent.i18n.DeveloperErrorMessageKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The API entry point handler - the 'home page' - responds only to GET, with a set of links for the first level of the API
 *
 * @see ApiModule
 */
@Singleton class RootHandler @Inject constructor(val errorResponseBuilder: ErrorResponseBuilder) : Handler {
    override fun handle(context: Context) {
        when (context.request.method) {
            HttpMethod.GET -> {
                val responseObject = HalResource()
                responseObject.self("/")
                responseObject.link(buildRequests, HalLink(buildRequests))
                context.response.status(200)
                context.render(json(responseObject))
            }
            else -> {
                val errorResponse = errorResponseBuilder.build(DeveloperErrorMessageKey.Invalid_Method, context.request.method, "/", "GET")
                context.render(json(errorResponse))
            }
        }

    }
}
