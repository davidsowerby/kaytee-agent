package uk.q3c.kaytee.agent.build

/**
 * Created by David Sowerby on 15 Jan 2017
 */
class BuildPreparationException(message: String, e: Exception?) : RuntimeException(message, e) {
    constructor (message: String) : this(message, null)
}