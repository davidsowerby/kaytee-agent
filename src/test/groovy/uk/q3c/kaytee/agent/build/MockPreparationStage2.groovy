package uk.q3c.kaytee.agent.build

import com.google.common.collect.ImmutableList
import com.google.inject.Inject
import org.gradle.tooling.UnsupportedVersionException
import org.jetbrains.annotations.NotNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import uk.q3c.kaytee.agent.eventbus.GlobalBusProvider
import uk.q3c.kaytee.agent.prepare.PreparationBuildStep
import uk.q3c.kaytee.agent.prepare.PreparationStage
import uk.q3c.kaytee.agent.queue.PreparationStartedMessage
import uk.q3c.kaytee.agent.queue.PreparationSuccessfulMessage
import uk.q3c.kaytee.agent.system.InstallationInfo
import uk.q3c.kaytee.plugin.KayTeeExtension
import uk.q3c.krail.core.i18n.I18NKey

/**
 * Created by David Sowerby on 28 Jan 2017
 */
class MockPreparationStage2 implements PreparationStage {

    private InstallationInfo installationInfo
    private GlobalBusProvider globalBusProvider
    private Logger log = LoggerFactory.getLogger(MockPreparationStage2)
    KayTeeExtension buildConfiguration = new KayTeeExtension()
    boolean failOnRun = false

    @Inject
    MockPreparationStage2(InstallationInfo installationInfo, GlobalBusProvider globalBusProvider) {
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
        log.info("Started preparation for build: {}", build.buildRunner.identity())
        globalBusProvider.get().publishAsync(new PreparationStartedMessage(build.buildRunner.uid, build.buildRunner.delegated))
        build.configure(buildConfiguration)
        if (failOnRun) {
            throw new UnsupportedVersionException("fake", new IOException())
        } else {
            globalBusProvider.get().publishAsync(new PreparationSuccessfulMessage(build.buildRunner.uid, build.buildRunner.delegated))
        }
        log.info("Completed preparation for build:  {}", build.buildRunner.identity())
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
