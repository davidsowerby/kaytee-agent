package uk.q3c.kaytee.agent.prepare

import com.google.inject.Inject
import org.apache.commons.io.FileUtils
import uk.q3c.kaytee.agent.build.Build
import uk.q3c.kaytee.agent.build.BuildPreparationException
import uk.q3c.kaytee.agent.i18n.LabelKey
import uk.q3c.kaytee.agent.i18n.Named
import uk.q3c.kaytee.agent.i18n.NamedFactory
import uk.q3c.kaytee.agent.system.InstallationInfo

/**
 * Created by David Sowerby on 19 Jan 2017
 */
class DefaultPrepareWorkspace @Inject constructor(val installationInfo: InstallationInfo, namedFactory: NamedFactory)
    : PrepareWorkspace, Named by namedFactory.create(LabelKey.Prepare_Workspace) {

    override fun execute(build: Build) {
        val codeArea = installationInfo.buildNumberDir(build)
        try {
            if (!codeArea.exists()) {
                FileUtils.forceMkdir(codeArea)
            }
        } catch (e: Exception) {
            val msg = "Workspace preparation failed for $codeArea"
            throw BuildPreparationException(msg, e)
        }
    }
}