package uk.q3c.simplycd.agent.i18n

/**
 * Created by David Sowerby on 03 May 2016
 */

import com.google.common.collect.ImmutableMap
import uk.q3c.kaytee.plugin.TaskNames.*
import uk.q3c.krail.core.i18n.I18NKey
import uk.q3c.simplycd.agent.i18n.TaskKey.*
import java.util.*

enum class TaskKey : I18NKey {
    Extract_Gradle_Configuration,
    Unit_Test,
    Integration_Test,
    Generate_Build_Info,
    Generate_Change_Log,
    Local_Publish,
    Functional_Test,
    Acceptance_Test,
    Merge_to_Master,
    Bintray_Upload,
    Production_Test,

}

enum class TaskQualityGateKey : I18NKey {
    Acceptance_Test_Quality_Gate,
    Integration_Test_Quality_Gate,
    Functional_Test_Quality_Gate,
    Production_Test_Quality_Gate,
    Unit_Test_Quality_Gate
}

val testTasks: EnumSet<TaskKey> = EnumSet.of(Unit_Test, Integration_Test, Functional_Test, Acceptance_Test, Production_Test)
val qualityGateLookup: ImmutableMap<TaskKey, TaskQualityGateKey> = ImmutableMap.of(
        Unit_Test, TaskQualityGateKey.Unit_Test_Quality_Gate,
        Integration_Test, TaskQualityGateKey.Integration_Test_Quality_Gate,
        Functional_Test, TaskQualityGateKey.Functional_Test_Quality_Gate,
        Acceptance_Test, TaskQualityGateKey.Acceptance_Test_Quality_Gate,
        Production_Test, TaskQualityGateKey.Production_Test_Quality_Gate
)

class TaskNameMap {
    val taskPhrases: ImmutableMap<TaskKey, String>
    val qualityGatePhrases: ImmutableMap<TaskQualityGateKey, String>


    init {
        val temp: MutableMap<TaskKey, String> = mutableMapOf()
        temp.put(Acceptance_Test, ACCEPTANCE_TEST)
        temp.put(Generate_Build_Info, GENERATE_BUILD_INFO_TASK_NAME)
        temp.put(Generate_Change_Log, GENERATE_CHANGE_LOG_TASK_NAME)
        temp.put(Extract_Gradle_Configuration, GENERATE_CONFIG_TASK_NAME)
        temp.put(Integration_Test, "clean $INTEGRATION_TEST")
        temp.put(Functional_Test, FUNCTIONAL_TEST)
        temp.put(Local_Publish, LOCAL_PUBLISH)
        temp.put(Production_Test, PRODUCTION_TEST)
        temp.put(Unit_Test, UNIT_TEST)

        temp.put(Merge_to_Master, MERGE_TO_MASTER)
        taskPhrases = ImmutableMap.copyOf(temp)

        val tempQG: MutableMap<TaskQualityGateKey, String> = mutableMapOf()
        tempQG.put(TaskQualityGateKey.Acceptance_Test_Quality_Gate, ACCEPTANCE_QUALITY_GATE)
        tempQG.put(TaskQualityGateKey.Integration_Test_Quality_Gate, INTEGRATION_QUALITY_GATE)
        tempQG.put(TaskQualityGateKey.Functional_Test_Quality_Gate, FUNCTIONAL_QUALITY_GATE)
        tempQG.put(TaskQualityGateKey.Production_Test_Quality_Gate, PRODUCTION_QUALITY_GATE)
        tempQG.put(TaskQualityGateKey.Unit_Test_Quality_Gate, UNIT_TEST_QUALITY_GATE)
        qualityGatePhrases = ImmutableMap.copyOf(tempQG)
    }


    fun commitStage(): EnumSet<TaskKey> {
        return EnumSet.of(Unit_Test, Integration_Test, Generate_Build_Info, Generate_Change_Log, Local_Publish)
    }

    fun functionalStage(): EnumSet<TaskKey> {
        return EnumSet.of(Functional_Test)
    }

    fun acceptanceStage(): EnumSet<TaskKey> {
        return EnumSet.of(Acceptance_Test)
    }

    fun productionStage(): EnumSet<TaskKey> {
        return EnumSet.of(Production_Test)
    }

    @JvmOverloads
    fun get(taskKey: TaskKey, qualityGate: Boolean = false): String {
        // we should never get a null as all [TaskKey[ and [TaskQualityGateKey] instances have an entry
        return if (qualityGate) {
            val qgKey: TaskQualityGateKey = qualityGateLookup.get(taskKey)!!
            get(qgKey)
        } else {
            taskPhrases[taskKey]!!
        }
    }

    fun get(taskKey: TaskQualityGateKey): String {
        // we should never get a null as all [TaskQualityGateKey] instances have an entry
        return qualityGatePhrases[taskKey]!!
    }
}


