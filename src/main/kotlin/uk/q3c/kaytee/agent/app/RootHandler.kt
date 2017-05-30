package uk.q3c.kaytee.agent.app

import ratpack.handling.Context
import ratpack.jackson.Jackson.json
import uk.q3c.rest.hal.HalLink
import uk.q3c.rest.hal.HalResource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The API entry point handler - the 'home page' - responds only to GET, with a set of links for the first level of the API
 *
 * @see ApiModule
 */
@Singleton class RootHandler @Inject constructor(errorResponseBuilder: ErrorResponseBuilder) : AbstractHandler(errorResponseBuilder) {

    override fun get(context: Context) {
        val responseObject = HalResource()
        responseObject.self("/")
        responseObject.link(buildRequests, HalLink(buildRequests))
        context.response.status(200)
        context.render(json(responseObject))
    }
}
