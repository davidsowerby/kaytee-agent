package uk.q3c.kaytee.agent.build

import uk.q3c.build.gitplus.remote.GPIssue

/**
 * Raises an issue in the remote Git provider (for example GitHub)
 *
 * Created by David Sowerby on 19 Jun 2017
 */
interface IssueCreator {
    fun raiseIssue(build: Build): GPIssue
}