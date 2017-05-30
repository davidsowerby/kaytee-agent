package uk.q3c.kaytee.agent.build

import uk.q3c.build.gitplus.GitSHA
import uk.q3c.kaytee.agent.project.Project
import uk.q3c.kaytee.agent.queue.BuildRunner
import java.util.*

/**
 * Created by David Sowerby on 27 Jan 2017
 */
interface BuildRequestFactory {

    fun create(project: Project, gitSHA: GitSHA, uid: UUID): BuildRunner
}