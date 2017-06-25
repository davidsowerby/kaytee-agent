package uk.q3c.kaytee.agent.build

import org.jetbrains.annotations.NotNull

/**
 * Created by David Sowerby on 19 Jun 2017
 */
class MockIssueCreator implements IssueCreator {
    def calls

    @Override
    void raiseIssue(@NotNull Build build) {
        calls++
    }
}
