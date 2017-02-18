package uk.q3c.simplycd.build

import uk.q3c.build.gitplus.GitSHA
import uk.q3c.simplycd.project.Project
import uk.q3c.simplycd.queue.BuildRequest

/**
 * Created by David Sowerby on 27 Jan 2017
 */
interface BuildRequestFactory {

    fun create(project: Project, gitSHA: GitSHA): BuildRequest
}