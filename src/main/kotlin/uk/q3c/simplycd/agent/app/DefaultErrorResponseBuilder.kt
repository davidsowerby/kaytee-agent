package uk.q3c.simplycd.agent.app

import com.google.inject.Inject
import ratpack.http.HttpMethod
import uk.q3c.krail.core.i18n.Translate
import uk.q3c.simplycd.agent.i18n.DeveloperErrorMessageKey
import uk.q3c.simplycd.agent.i18n.DeveloperErrorMessageKey.Invalid_Method
import uk.q3c.simplycd.agent.i18n.UserErrorMessageKey

/**
 * Created by David Sowerby on 04 Mar 2017
 */
class DefaultErrorResponseBuilder @Inject constructor(val translate: Translate) : ErrorResponseBuilder {
    override fun invalidMethod(uri: String, calledMethod: HttpMethod, validMethods: List<HttpMethod>): ErrorResponse {
        val developerMessage = translate.from(Invalid_Method, calledMethod, uri, validMethods)
        val userMessageKey = UserErrorMessageKey.valueOf(Invalid_Method.name)
        val userMessage = translate.from(userMessageKey, calledMethod, uri, validMethods)
        val errorResponse = ErrorResponse(Invalid_Method.httpCode, Invalid_Method.name, userMessage, developerMessage)
        return errorResponse
    }

    override fun build(messageKey: DeveloperErrorMessageKey, vararg args: Any): ErrorResponse {
        val developerMessage = translate.from(messageKey, *args)
        val userMessageKey = UserErrorMessageKey.valueOf(messageKey.name)
        val userMessage = translate.from(userMessageKey, *args)
        val errorResponse = ErrorResponse(messageKey.httpCode, messageKey.name, userMessage, developerMessage)
        return errorResponse
    }
}