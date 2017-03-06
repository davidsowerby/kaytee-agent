package uk.q3c.simplycd.agent.api

import com.google.inject.Inject
import uk.q3c.krail.core.i18n.Translate
import uk.q3c.simplycd.agent.i18n.DeveloperErrorMessageKey
import uk.q3c.simplycd.agent.i18n.UserErrorMessageKey

/**
 * Created by David Sowerby on 04 Mar 2017
 */
class DefaultErrorResponseBuilder @Inject constructor(val translate: Translate) : ErrorResponseBuilder {

    override fun build(messageKey: DeveloperErrorMessageKey, vararg params: Any): ErrorResponse {
        val developerMessage = translate.from(messageKey, params)
        val userMessageKey = UserErrorMessageKey.valueOf(messageKey.name)
        val userMessage = translate.from(userMessageKey, params)
        val errorResponse = ErrorResponse(messageKey.httpCode, messageKey.name, userMessage, developerMessage)
        return errorResponse
    }
}