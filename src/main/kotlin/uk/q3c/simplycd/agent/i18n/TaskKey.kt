package uk.q3c.simplycd.agent.i18n

/**
 * Created by David Sowerby on 03 May 2016
 */

import com.google.common.collect.ImmutableMap
import uk.q3c.krail.core.i18n.I18NKey
import uk.q3c.simplycd.agent.i18n.TaskKey.*
import uk.q3c.simplycd.lifecycle.TaskNames.*
import java.util.*

enum class TaskKey : I18NKey {
    Acceptance_Test,
    Acceptance_Test_Quality_Gate,
    Build_Info,
    Change_Log,
    Extract_Gradle_Configuration,
    Integration_Test,
    Integration_Test_Quality_Gate,
    Functional_Test,
    Functional_Test_Quality_Gate,
    Local_Publish,
    Production_Test,
    Production_Test_Quality_Gate,
    Unit_Test,
    Unit_Test_Quality_Gate
}


class TaskNameMap {
    val names: Map<TaskKey, String>

    init {
        val temp: MutableMap<TaskKey, String> = mutableMapOf()
        temp.put(Acceptance_Test, ACCEPTANCE_TEST)
        temp.put(Acceptance_Test_Quality_Gate, ACCEPTANCE_QUALITY_GATE)
        temp.put(Build_Info, CREATE_BUILD_INFO_TASK_NAME)
        temp.put(Change_Log, GENERATE_CHANGE_LOG_TASK_NAME)
        temp.put(Extract_Gradle_Configuration, GENERATE_CONFIG_TASK_NAME)
        temp.put(Integration_Test, "clean $INTEGRATION_TEST")
        temp.put(Integration_Test_Quality_Gate, INTEGRATION_QUALITY_GATE)
        temp.put(Functional_Test, FUNCTIONAL_TEST)
        temp.put(Functional_Test_Quality_Gate, FUNCTIONAL_QUALITY_GATE)
        temp.put(Local_Publish, LOCAL_PUBLISH)
        temp.put(Production_Test, PRODUCTION_TEST)
        temp.put(Production_Test_Quality_Gate, PRODUCTION_QUALITY_GATE)
        temp.put(Unit_Test, UNIT_TEST)
        temp.put(Unit_Test_Quality_Gate, UNIT_TEST_QUALITY_GATE)
        names = ImmutableMap.copyOf(temp)
    }

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

    fun get(taskKey: TaskKey): String {
        // we should never get a null as all TaskKey instances have an entry
        return names[taskKey]!!
    }
}


