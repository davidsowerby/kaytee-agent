package uk.q3c.kaytee.agent.build

import org.jetbrains.annotations.NotNull
import uk.q3c.build.gitplus.remote.GPIssue
/**
 * Created by David Sowerby on 19 Jun 2017
 */
class MockIssueCreator implements IssueCreator {


    int calls

    @Override
    GPIssue raiseIssue(@NotNull Build build) {
        calls++
        return new GPIssue(calls)
    }
}
