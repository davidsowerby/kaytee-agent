package uk.q3c.simplycd.lifecycle.prepare

import com.google.inject.Inject
import org.apache.commons.io.FileUtils
import uk.q3c.simplycd.build.Build
import uk.q3c.simplycd.build.BuildPreparationException
import uk.q3c.simplycd.i18n.LabelKey
import uk.q3c.simplycd.i18n.Named
import uk.q3c.simplycd.i18n.NamedFactory
import uk.q3c.simplycd.system.InstallationInfo

/**
 * Created by David Sowerby on 19 Jan 2017
 */
class DefaultPrepareWorkspace @Inject constructor(val installationInfo: InstallationInfo, namedFactory: NamedFactory)
    : PrepareWorkspace, Named by namedFactory.create(LabelKey.Prepare_Workspace) {

    override fun execute(build: Build) {
        val codeArea = installationInfo.codeDir(build)
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