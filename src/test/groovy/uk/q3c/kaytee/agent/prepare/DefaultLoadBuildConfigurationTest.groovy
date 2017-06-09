package uk.q3c.kaytee.agent.prepare

import org.apache.commons.io.FileUtils
import uk.q3c.kaytee.agent.build.Build
import uk.q3c.kaytee.agent.build.BuildPreparationException
import uk.q3c.kaytee.agent.i18n.LabelKey
import uk.q3c.kaytee.agent.queue.GradleTaskExecutor
import uk.q3c.kaytee.agent.queue.GradleTaskRunner
import uk.q3c.kaytee.plugin.KayTeeExtension
import uk.q3c.util.testutil.TestResource

import static uk.q3c.kaytee.plugin.TaskKey.*
/**
 * Created by David Sowerby on 20 Jan 2017
 */
class DefaultLoadBuildConfigurationTest extends PreparationStepSpecification {


    DefaultLoadBuildConfiguration step
    GradleTaskExecutor gradleTaskExecutor = Mock(GradleTaskExecutor)
    GradleTaskRunner gradleTaskRequest = Mock(GradleTaskRunner)


    def setup() {
        step = new DefaultLoadBuildConfiguration(installationInfo, gradleTaskExecutor, i18NNamedFactory)

    }

    def "execute error throws preparation exception"() {

        when:
        step.execute(build)

        then:
        installationInfo.buildNumberDir(build) >> { throw new NullPointerException("boo!") }

        then:
        thrown BuildPreparationException
    }

    def "name"() {

        when:
        step = new DefaultLoadBuildConfiguration(installationInfo, gradleTaskExecutor, i18NNamedFactory)

        then:
        1 * i18NNamedFactory.create(LabelKey.Load_Configuration)
    }

    def "execute successfully"() {
        given:
        GradleTaskRunner gradleTaskRequest = Mock(GradleTaskRunner)
        gradleTaskRunnerFactory.create(_, _, false) >> gradleTaskRequest
        File ref = TestResource.resource(this, 'kaytee.json')
        File target = new File(codeDir, "build/kaytee.json")
        FileUtils.copyFile(ref, target)

        when:
        step.execute(build)

        then: "we should get a gradleTaskRequestFactory.create for each task that is not disabled"
        installationInfo.projectInstanceDir(build) >> codeDir
        1 * gradleTaskExecutor.execute(build, Extract_Gradle_Configuration, false)
        1 * gradleTaskRunnerFactory.create(build, Unit_Test, true) >> this.gradleTaskRequest
        1 * gradleTaskRunnerFactory.create(build, Integration_Test, true) >> this.gradleTaskRequest
        1 * gradleTaskRunnerFactory.create(build, Functional_Test, true) >> this.gradleTaskRequest
        1 * gradleTaskRunnerFactory.create(build, Generate_Build_Info, false) >> this.gradleTaskRequest
        0 * gradleTaskRunnerFactory.create(build, Generate_Change_Log, true) >> this.gradleTaskRequest
        1 * gradleTaskRunnerFactory.create(build, Local_Publish, false) >> this.gradleTaskRequest
        0 * gradleTaskRunnerFactory.create(build, Merge_to_Master, false) >> this.gradleTaskRequest
        0 * gradleTaskRunnerFactory.create(build, Bintray_Upload, false) >> this.gradleTaskRequest
    }

    def "values correct"() {
        given:
        GradleTaskRunner gradleTaskRequest = Mock(GradleTaskRunner)
        gradleTaskRunnerFactory.create(_, _, false) >> gradleTaskRequest
        File ref = TestResource.resource(this, 'kaytee.json')
        File target = new File(codeDir, "build/kaytee.json")
        FileUtils.copyFile(ref, target)
        KayTeeExtension config
        build = Mock(Build)

        when:
        step.execute(build)

        then:
        1 * installationInfo.projectInstanceDir(build) >> codeDir
        1 * build.configure(_) >> { arguments -> config = arguments[0] }
        config.integrationTest.enabled
        config.integrationTest.qualityGate

        !config.acceptanceTest.enabled
        !config.acceptanceTest.qualityGate
    }
}
