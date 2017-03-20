package uk.q3c.simplycd.agent.app

import ratpack.handling.Context
import ratpack.handling.Handler

/**
 * An example of a handler implicitly set up by a module
 *
 * @see MyModule
 */
class LoggingHandler : Handler {
    override fun handle(ctx: Context) {
        println("Received: ${ctx.request.uri}")
        ctx.next()
    }
}
