package uk.q3c.kaytee.agent.prepare

import com.google.inject.Inject
import uk.q3c.kaytee.agent.build.Build
import uk.q3c.kaytee.agent.build.BuildPreparationException
import uk.q3c.kaytee.agent.i18n.LabelKey
import uk.q3c.kaytee.agent.i18n.Named
import uk.q3c.kaytee.agent.i18n.NamedFactory
import uk.q3c.kaytee.agent.system.InstallationInfo
import uk.q3c.util.file.FileKUtils
import java.io.File

/**
 * Created by David Sowerby on 19 Jan 2017
 */
class DefaultPrepareWorkspace @Inject constructor(val installationInfo: InstallationInfo, val fileUtils: FileKUtils, namedFactory: NamedFactory)
    : PrepareWorkspace, Named by namedFactory.create(LabelKey.Prepare_Workspace) {

    override fun execute(build: Build) {
        val codeArea = installationInfo.buildNumberDir(build)
        try {
            if (codeArea.exists()) {
                renamePreviousBuildArea(codeArea, build)
            }
            fileUtils.forceMkdir(codeArea)
        } catch (e: Exception) {
            val msg = "Workspace preparation failed for $codeArea"
            throw BuildPreparationException(msg, e)
        }
    }

    private fun renamePreviousBuildArea(codeArea: File, build: Build) {
        var newName = codeArea
        val baseFileName = newName.nameWithoutExtension
        var counter = 0
        while (newName.exists()) {
            newName = File(installationInfo.projectDir(build), "$baseFileName-$counter")
            counter++
        }
        val resultOk = codeArea.renameTo(newName)
        if (!resultOk) {
            throw BuildPreparationException("Unable to rename previous build area to $newName")
        }
    }
}