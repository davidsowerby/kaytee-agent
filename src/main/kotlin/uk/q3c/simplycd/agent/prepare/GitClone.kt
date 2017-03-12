package uk.q3c.simplycd.agent.prepare

/**
 * A preparation build step responsible for cloning the project to be built.  Note that it also checks out the appropriate ref (Git hash)

 * Created by David Sowerby on 13 May 2016
 */
interface GitClone : PreparationBuildStep

