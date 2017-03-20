package uk.q3c.simplycd.agent.app

import ratpack.handling.Context
import ratpack.handling.Handler
import ratpack.http.HttpMethod
import ratpack.jackson.Jackson.json
import uk.q3c.rest.hal.HalLink
import uk.q3c.rest.hal.HalResource
import uk.q3c.simplycd.agent.invalidMethod
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The API entry point handler - the 'home page' - responds only to GET, with a set of links for the first level of the API
 *
 * @see ApiModule
 */
@Singleton class RootHandler @Inject constructor(val errorResponseBuilder: ErrorResponseBuilder) : Handler {
    override fun handle(ctx: Context) {
        when (ctx.request.method) {
            HttpMethod.GET -> {
                val responseObject = HalResource()
                responseObject.self("/")
                responseObject.link(buildRequests, HalLink(buildRequests))
                ctx.response.status(200)
                ctx.render(json(responseObject))
            }
            else -> {
                invalidMethod(context = ctx, builder = errorResponseBuilder, allowedMethods = arrayOf("GET"))
            }
        }
    }
}
