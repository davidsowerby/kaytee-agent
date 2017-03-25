package uk.q3c.simplycd.agent.prepare

/**
 *
 * We cannot access Gradle extensions via the Gradle Tools API, so we extract the SimplyCD configuration by using
 * the *simplycdConfigToJson* task provided by Plugin *simplycd-lifecycle"
 * Created by David Sowerby on 19 Jan 2017
 */
interface LoadBuildConfiguration : PreparationBuildStep