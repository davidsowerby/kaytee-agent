package uk.q3c.simplycd.agent.api

import com.google.inject.Inject
import uk.q3c.simplycd.agent.i18n.DeveloperErrorMessageKey
import uk.q3c.simplycd.agent.i18n.DeveloperErrorMessages
import uk.q3c.simplycd.agent.i18n.UserErrorMessageKey
import uk.q3c.simplycd.agent.i18n.UserErrorMessages
import uk.q3c.simplycd.agent.i18n.lib.DefaultMessageFormat
import uk.q3c.simplycd.agent.i18n.lib.MessageFormatMode.STRICT

/**
 * Created by David Sowerby on 04 Mar 2017
 */
class DefaultErrorResponseBuilder @Inject constructor() : ErrorResponseBuilder {

    override fun build(messageKey: DeveloperErrorMessageKey, vararg args: Any): ErrorResponse {
        val developerMessages = DeveloperErrorMessages()
        developerMessages.keyClass = DeveloperErrorMessageKey::class.java
        developerMessages.load()
        val developerMessagePattern = developerMessages.getValue(messageKey)
        val developerMessage = DefaultMessageFormat.format(STRICT, developerMessagePattern!!, *args)

        val userMessages = UserErrorMessages()
        userMessages.keyClass = UserErrorMessageKey::class.java
        userMessages.load()
        val userMessageKey = UserErrorMessageKey.valueOf(messageKey.name)
        val userMessagePattern = userMessages.getValue(userMessageKey)
        val userMessage = DefaultMessageFormat.format(STRICT, userMessagePattern!!, *args)
        val errorResponse = ErrorResponse(messageKey.httpCode, messageKey.name, userMessage, developerMessage)
        return errorResponse
    }
}