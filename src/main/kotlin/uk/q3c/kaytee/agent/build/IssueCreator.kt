package uk.q3c.kaytee.agent.build

/**
 * Raises an issue in the event of a build failure
 *
 * Created by David Sowerby on 19 Jun 2017
 */
interface IssueCreator {
    fun raiseIssue(build: Build)
}