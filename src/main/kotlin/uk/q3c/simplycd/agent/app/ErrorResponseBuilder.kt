package uk.q3c.simplycd.agent.app

import uk.q3c.simplycd.agent.i18n.DeveloperErrorMessageKey

/**
 * Created by David Sowerby on 04 Mar 2017
 */
interface ErrorResponseBuilder {
    fun build(messageKey: DeveloperErrorMessageKey, vararg args: Any): ErrorResponse
}