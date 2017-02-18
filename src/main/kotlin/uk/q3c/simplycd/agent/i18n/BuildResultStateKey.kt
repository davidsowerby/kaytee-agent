package uk.q3c.simplycd.i18n

import uk.q3c.krail.core.i18n.I18NKey

/**
 * See [BuildResultStates]
 *
 * Created by David Sowerby on 03 May 2016
 */
enum class BuildResultStateKey : I18NKey {
    Build_Cancelled,
    Build_Failure,
    Build_Successful,
    Gradle_Connection_Failure,
    Gradle_Illegal_State,
    Preparation_Failed,
    Unsupported_Build_Argument,
    Unexpected_Exception_Type,
    Unsupported_Gradle_Version,
    Unsupported_Operation_Configuration,

}
