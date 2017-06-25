package uk.q3c.kaytee.agent.prepare

import uk.q3c.kaytee.plugin.TaskNames
/**
 *
 * Calls the plugin task 'versionCheck' to ensure that we are not trying to build a version which already exists (that is,
 * there is already a Git tag in this repo with the same base version).  This allows a 'fail early', instead of waiting
 * until we come to tag (at release time) to find out the tag has already been used
 *
 * We cannot access Gradle extensions via the Gradle Tools API, so we extract the KayTee configuration by using
 * the [TaskNames.GENERATE_CONFIG_TASK_NAME] task provided by Gradle plugin *kaytee-plugin*
 * Created by David Sowerby on 19 Jan 2017
 */
interface LoadBuildConfiguration : PreparationBuildStep