package uk.q3c.kaytee.agent.build

import uk.q3c.build.gitplus.remote.GPIssue

/**
 * Raises an issue in the remote Git provider (for example GitHub)
 *
 * Created by David Sowerby on 19 Jun 2017
 */
interface IssueCreator {

    /**
     * Raises an issue, returning that issue or an issue with number of '0' if the issue creation failed
     *
     * @return the raised issue, an issue with number 0 if the creation failed
     */
    fun raiseIssue(build: Build): GPIssue
}