package uk.q3c.kaytee.agent.build

import uk.q3c.kaytee.agent.i18n.BuildStateKey

/**
 * Created by David Sowerby on 17 Jul 2017
 */
class DefaultStateModel : StateModel {

    override fun currentStateValid(currentState: BuildStateKey, newState: BuildStateKey): Boolean {
        when (newState) {
            BuildStateKey.Not_Started -> throw InvalidBuildStateException("Cannot update the build record to be NOT STARTED")
            BuildStateKey.Requested -> return currentState == BuildStateKey.Not_Started
            BuildStateKey.Preparation_Started -> return currentState == BuildStateKey.Requested
            BuildStateKey.Preparation_Successful -> return currentState == BuildStateKey.Preparation_Started
            BuildStateKey.Preparation_Failed -> return currentState == BuildStateKey.Preparation_Started
            BuildStateKey.Started -> return currentState == BuildStateKey.Preparation_Successful
            BuildStateKey.Cancelled -> TODO()
            BuildStateKey.Failed -> return currentState == BuildStateKey.Started
            BuildStateKey.Successful -> return currentState == BuildStateKey.Started
            BuildStateKey.Complete -> return currentState == BuildStateKey.Preparation_Failed || currentState == BuildStateKey.Failed || currentState == BuildStateKey.Successful
        }
    }
}