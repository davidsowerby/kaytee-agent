package uk.q3c.kaytee.agent.i18n

import uk.q3c.kaytee.agent.i18n.BuildStateKey.*
import uk.q3c.krail.core.i18n.I18NKey
import java.util.*

/**
 * See [BuildFailCauses]
 *
 * Created by David Sowerby on 03 May 2016
 */
enum class BuildFinalStateKey : I18NKey

enum class BuildStateKey : I18NKey {
    Not_Started,
    Requested,
    Preparation_Started,
    Preparation_Successful,
    Started,

    Cancelled,
    Failed,
    Successful,
}

val finalStates: EnumSet<BuildStateKey> = EnumSet.of(Cancelled, Failed, Successful)

enum class BuildFailCauseKey : I18NKey {
    Unsupported_Build_Argument,
    Unexpected_Exception_Type,
    Unsupported_Gradle_Version,
    Unsupported_Operation_Configuration,
    Gradle_Connection_Failure,
    Gradle_Illegal_State,

    Build_Failed,

    Build_Cancelled,

    Preparation_Failed,

    Build_Configuration,
    Not_Applicable,

    Task_Failure
}

enum class TaskStateKey : I18NKey {
    Cancelled,
    Failed,
    Not_Run,
    Successful,
    Requested,
    Quality_Gate_Failed,

    Started,

    Not_Required
}
