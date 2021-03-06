package uk.q3c.kaytee.agent.app

import com.google.inject.Inject
import ratpack.http.HttpMethod
import uk.q3c.kaytee.agent.i18n.DeveloperErrorMessageKey
import uk.q3c.kaytee.agent.i18n.DeveloperErrorMessageKey.Invalid_Method
import uk.q3c.kaytee.agent.i18n.UserErrorMessageKey
import uk.q3c.krail.i18n.Translate

/**
 * Created by David Sowerby on 04 Mar 2017
 */
class DefaultErrorResponseBuilder @Inject constructor(val translate: Translate, val expandedPublicAddress: ExpandedPublicAddress) : ErrorResponseBuilder {


    override fun invalidMethod(uri: String, calledMethod: HttpMethod, validMethods: List<HttpMethod>): ErrorResponse {
        val developerMessage = translate.from(Invalid_Method, calledMethod, uri, validMethods)
        val userMessageKey = UserErrorMessageKey.valueOf(Invalid_Method.name)
        val userMessage = translate.from(userMessageKey, calledMethod, uri, validMethods)
        return createResponse(Invalid_Method, userMessage, developerMessage)
    }

    override fun build(uri: String, messageKey: DeveloperErrorMessageKey, vararg args: Any): ErrorResponse {
        val developerMessage = translate.from(messageKey, *args)
        val userMessageKey = UserErrorMessageKey.valueOf(messageKey.name)
        val userMessage = translate.from(userMessageKey, *args)
        return createResponse(messageKey, userMessage, developerMessage)
    }

    private fun createResponse(messageKey: DeveloperErrorMessageKey, userMessage: String, developerMessage: String): ErrorResponse {
        val errorResponse = ErrorResponse(messageKey.httpCode, messageKey.name, userMessage, developerMessage)
        errorResponse.self(expandedPublicAddress.errorDocUrl(messageKey).toString())
        return errorResponse
    }


}