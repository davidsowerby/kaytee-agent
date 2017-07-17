package uk.q3c.kaytee.agent.build

import uk.q3c.kaytee.agent.i18n.BuildStateKey

/**
 * Created by David Sowerby on 17 Jul 2017
 */
interface StateModel {
    fun currentStateValid(currentState: BuildStateKey, newState: BuildStateKey): Boolean
}