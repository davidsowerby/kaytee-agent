package uk.q3c.simplycd.agent.i18n

/**
 * Created by David Sowerby on 03 May 2016
 */

import uk.q3c.krail.core.i18n.I18NKey
import uk.q3c.simplycd.lifecycle.TaskNames.*
import java.util.*

enum class TaskKey : I18NKey {
    Acceptance_Test {
        override fun command() = ACCEPTANCE_TEST
    },
    Acceptance_Test_Quality_Gate {
        override fun command() = ACCEPTANCE_QUALITY_GATE
    },
    Build_Info {
        override fun command() = CREATE_BUILD_INFO_TASK_NAME
    },
    Change_Log {
        override fun command() = GENERATE_CHANGE_LOG_TASK_NAME
    },
    Extract_Gradle_Configuration {
        override fun command() = GENERATE_CONFIG_TASK_NAME
    },
    Integration_Test {
        override fun command() = "clean ${INTEGRATION_TEST}"
    },
    Integration_Test_Quality_Gate {
        override fun command() = INTEGRATION_QUALITY_GATE
    },
    Functional_Test {
        override fun command() = FUNCTIONAL_TEST
    },
    Functional_Test_Quality_Gate {
        override fun command() = FUNCTIONAL_QUALITY_GATE
    },
    Local_Publish {
        override fun command() = LOCAL_PUBLISH
    },
    Production_Test {
        override fun command() = PRODUCTION_TEST
    },
    Production_Test_Quality_Gate {
        override fun command() = PRODUCTION_QUALITY_GATE
    },
    Unit_Test {
        override fun command() = "clean ${UNIT_TEST}"
    },
    Unit_Test_Quality_Gate {
        override fun command() = UNIT_TEST_QUALITY_GATE
    };

    abstract fun command(): String

    fun commitStage(): EnumSet<TaskKey> {
        return EnumSet.of(Unit_Test, Unit_Test_Quality_Gate, Integration_Test, Integration_Test_Quality_Gate, Build_Info, Change_Log, Local_Publish)
    }

    fun functionalStage(): EnumSet<TaskKey> {
        return EnumSet.of(Functional_Test, Functional_Test_Quality_Gate)
    }

    fun acceptanceStage(): EnumSet<TaskKey> {
        return EnumSet.of(Acceptance_Test, Acceptance_Test_Quality_Gate)
    }

    fun productionStage(): EnumSet<TaskKey> {
        return EnumSet.of(Production_Test, Production_Test_Quality_Gate)
    }


}


