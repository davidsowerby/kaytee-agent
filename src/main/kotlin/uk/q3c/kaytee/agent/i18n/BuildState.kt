package uk.q3c.kaytee.agent.i18n

import uk.q3c.kaytee.agent.queue.BuildFailedMessage
import uk.q3c.kaytee.agent.queue.BuildSuccessfulMessage
import uk.q3c.krail.i18n.I18NKey

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
    Preparation_Failed,
    Started,

    Cancelled,
    Failed,
    Successful,

    /**
     * All processing completed when true, including any post processing done after [BuildSuccessfulMessage] or [BuildFailedMessage] received
     */
    Complete
}


enum class BuildFailCauseKey : I18NKey {
    Unsupported_Build_Argument,
    Unexpected_Exception_Type,
    Unsupported_Gradle_Version,
    Unsupported_Operation_Configuration,
    Gradle_Connection_Failure,
    Gradle_Illegal_State,

    Build_Failed,

    Build_Cancelled,

    Preparation_Failure,

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
