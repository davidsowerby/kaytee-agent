package uk.q3c.simplycd.agent.app

import com.google.common.collect.ImmutableList
import org.slf4j.LoggerFactory
import ratpack.handling.Context
import ratpack.handling.Handler
import ratpack.http.HttpMethod
import ratpack.http.HttpMethod.*
import ratpack.jackson.Jackson
import uk.q3c.simplycd.agent.i18n.DeveloperErrorMessageKey

/**
 * Standardises handling of incorrect method calls and exceptions
 *
 * Created by David Sowerby on 19 Mar 2017
 */
abstract class AbstractHandler(val errorResponseBuilder: ErrorResponseBuilder) : Handler {
    private val log = LoggerFactory.getLogger(this.javaClass.name)
    protected var validMethodCalls: ImmutableList<HttpMethod> = ImmutableList.of(GET)


    override fun handle(ctx: Context) {
        try {
            val method = ctx.request.method
            log.debug("${method.name} received")
            if (validMethodCalls.contains(method)) {
                when (ctx.request.method) {
                    POST -> post(ctx)
                    GET -> get(ctx)
                    PUT -> put(ctx)
                    PATCH -> patch(ctx)
                    DELETE -> delete(ctx)
                    OPTIONS -> options(ctx)
                }
            } else {
                log.debug("Method '{}' is not valid for this handler", method.name)
                val errorResponse = errorResponseBuilder.invalidMethod(ctx.request.uri, method, validMethodCalls)
                ctx.render(Jackson.json(errorResponse))

            }

        } catch(e: Exception) {
            log.error("Error occurred in handler response", e)
            val errorResponse = errorResponseBuilder.build(DeveloperErrorMessageKey.Exception_in_Handler, e.message ?: "no message")
            ctx.response.status(DeveloperErrorMessageKey.Exception_in_Handler.httpCode)
            ctx.render(Jackson.json(errorResponse))
        }

    }


    open fun post(context: Context) {
        throw HandlerDefinitionException(POST)
    }

    open fun get(context: Context) {
        throw HandlerDefinitionException(GET)
    }

    open fun put(context: Context) {
        throw HandlerDefinitionException(PUT)
    }

    open fun patch(context: Context) {
        throw HandlerDefinitionException(PATCH)
    }

    open fun delete(context: Context) {
        throw HandlerDefinitionException(DELETE)
    }

    open fun options(context: Context) {
        throw HandlerDefinitionException(OPTIONS)
    }

}

class HandlerDefinitionException(method: HttpMethod) : RuntimeException("Either override this method to respond or modify 'validMethodCalls' to exclude ${method.name}")
