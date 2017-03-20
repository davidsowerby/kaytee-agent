package uk.q3c.simplycd.agent

import ratpack.handling.Context
import ratpack.jackson.Jackson
import uk.q3c.simplycd.agent.app.ErrorResponseBuilder
import uk.q3c.simplycd.agent.i18n.DeveloperErrorMessageKey

/**
 * Created by David Sowerby on 15 Mar 2017
 */

fun invalidMethod(context: Context, builder: ErrorResponseBuilder, allowedMethods: Array<String>) {
    val errorResponse = builder.build(DeveloperErrorMessageKey.Invalid_Method, context.request.method, context.pathBinding.boundTo, allowedMethods)
    context.render(Jackson.json(errorResponse))
}