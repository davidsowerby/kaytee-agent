package uk.q3c.simplycd.agent.build

import com.google.common.collect.ImmutableList
import com.google.inject.Inject
import org.apache.commons.io.FileUtils
import org.jetbrains.annotations.NotNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import uk.q3c.krail.core.i18n.I18NKey
import uk.q3c.simplycd.agent.eventbus.GlobalBusProvider
import uk.q3c.simplycd.agent.prepare.PreparationBuildStep
import uk.q3c.simplycd.agent.prepare.PreparationStage
import uk.q3c.simplycd.agent.queue.PreparationCompletedMessage
import uk.q3c.simplycd.agent.queue.PreparationStartedMessage
import uk.q3c.simplycd.agent.system.InstallationInfo

/**
 * Created by David Sowerby on 28 Jan 2017
 */
class MockPreparationStage implements PreparationStage {

    private InstallationInfo installationInfo
    private GlobalBusProvider globalBusProvider
    private Logger log = LoggerFactory.getLogger(MockPreparationStage)

    @Inject
    MockPreparationStage(InstallationInfo installationInfo, GlobalBusProvider globalBusProvider) {
        this.globalBusProvider = globalBusProvider
        this.installationInfo = installationInfo
    }

    @Override
    ImmutableList<PreparationBuildStep> getSteps() {
        return null
    }

    @Override
    void setSteps(@NotNull ImmutableList<PreparationBuildStep> immutableList) {

    }

    @Override
    void execute(@NotNull Build build) {
        log.info("Started preparation for build: {}", build.buildRequest.identity())
        globalBusProvider.get().publish(new PreparationStartedMessage(build.buildRequest))
        File outputDir = installationInfo.gradleOutputDir(build)
        if (!outputDir.exists()) {
            FileUtils.forceMkdir(outputDir)
        }
        build.gradleLauncher = new MockGradleLauncher()
        build.configure(new BuildConfigurationRandomiser().getConfig())
        File stdErr = installationInfo.gradleStdErrFile(build)
        if (!stdErr.exists()) {
            stdErr.createNewFile()
        }
        build.stderrOutputFile = stdErr

        File stdOut = installationInfo.gradleStdOutFile(build)
        if (!stdOut.exists()) {
            stdOut.createNewFile()
        }
        build.stdoutOutputFile = stdOut
        globalBusProvider.get().publish(new PreparationCompletedMessage(build.buildRequest))
        log.info("Completed preparation for build:  {}", build.buildRequest.identity())
    }

    @Override
    I18NKey getNameKey() {
        return null
    }

    @Override
    String name() {
        return null
    }
}
