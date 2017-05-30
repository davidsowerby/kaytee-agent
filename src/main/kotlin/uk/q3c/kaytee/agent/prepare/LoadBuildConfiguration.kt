package uk.q3c.kaytee.agent.prepare

import uk.q3c.kaytee.plugin.TaskNames
/**
 *
 * We cannot access Gradle extensions via the Gradle Tools API, so we extract the KayTee configuration by using
 * the [TaskNames.GENERATE_CONFIG_TASK_NAME] task provided by Gradle plugin *kaytee-plugin*
 * Created by David Sowerby on 19 Jan 2017
 */
interface LoadBuildConfiguration : PreparationBuildStep