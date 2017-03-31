package uk.q3c.simplycd.agent.queue


/**
 * Created by David Sowerby on 09 Feb 2017
 */
interface ManualTaskLauncher {

    fun run(manualTaskRequest: ManualTaskRunner)
}