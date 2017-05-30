package uk.q3c.kaytee.agent.app

import ratpack.http.HttpMethod
import uk.q3c.kaytee.agent.i18n.DeveloperErrorMessageKey

/**
 * Created by David Sowerby on 04 Mar 2017
 */
interface ErrorResponseBuilder {
    fun build(uri: String, messageKey: DeveloperErrorMessageKey, vararg args: Any): ErrorResponse
    fun invalidMethod(uri: String, calledMethod: HttpMethod, validMethods: List<HttpMethod>): ErrorResponse
}