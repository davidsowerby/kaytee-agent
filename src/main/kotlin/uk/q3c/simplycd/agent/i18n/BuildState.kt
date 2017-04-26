package uk.q3c.simplycd.agent.i18n

import uk.q3c.krail.core.i18n.I18NKey
import uk.q3c.simplycd.agent.i18n.BuildStateKey.*
import java.util.*

/**
 * See [BuildFailCauses]
 *
 * Created by David Sowerby on 03 May 2016
 */
enum class BuildFinalStateKey : I18NKey

enum class BuildStateKey : I18NKey {
    Not_Started,

    Preparation_Started,
    Preparation_Successful,
    Build_Started,

    Cancelled,
    Failed,
    Successful,
    Preparation_Failed,
}

val finalStates: EnumSet<BuildStateKey> = EnumSet.of(Cancelled, Failed, Successful, Preparation_Failed)

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

enum class TaskResultStateKey : I18NKey {
    Task_Cancelled,
    Task_Failed,
    Task_Not_Run,
    Task_Successful,
    Quality_Gate_Failed
}
