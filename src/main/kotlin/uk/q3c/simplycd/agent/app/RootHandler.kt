package uk.q3c.simplycd.agent.app

import ratpack.handling.Context
import ratpack.handling.Handler
import ratpack.jackson.Jackson.json
import uk.q3c.rest.hal.HalLink
import uk.q3c.rest.hal.HalResource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A handler implementation that is created via dependency injection.
 *
 * @see MyModule
 */
@Singleton class RootHandler @Inject constructor() : Handler {
    override fun handle(context: Context) {
        // build the API entry point response - the 'home page'
        val responseObject = HalResource()
        responseObject.self("/")
        responseObject.link("buildRequests", HalLink("buildRequests"))
        context.response.status(200)
        context.render(json(responseObject))
    }
}
