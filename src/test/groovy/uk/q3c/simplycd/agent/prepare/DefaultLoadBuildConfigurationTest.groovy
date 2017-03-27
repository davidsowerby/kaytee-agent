package uk.q3c.simplycd.agent.prepare

import org.apache.commons.io.FileUtils
import uk.q3c.simplycd.agent.build.BuildPreparationException
import uk.q3c.simplycd.agent.i18n.LabelKey
import uk.q3c.simplycd.agent.queue.GradleTaskExecutor
import uk.q3c.simplycd.agent.queue.GradleTaskRequest
import uk.q3c.util.testutil.TestResource
/**
 * Created by David Sowerby on 20 Jan 2017
 */
class DefaultLoadBuildConfigurationTest extends PreparationStepSpecification {


    DefaultLoadBuildConfiguration step
    GradleTaskExecutor gradleTaskExecutor = Mock(GradleTaskExecutor)
    GradleTaskRequest gradleTaskRequest = Mock(GradleTaskRequest)


    def setup() {
        step = new DefaultLoadBuildConfiguration(installationInfo, gradleTaskRequestFactory, gradleTaskExecutor, i18NNamedFactory)

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
        step = new DefaultLoadBuildConfiguration(installationInfo, gradleTaskRequestFactory, gradleTaskExecutor, i18NNamedFactory)

        then:
        1 * i18NNamedFactory.create(LabelKey.Load_Configuration)
    }

    def "execute successfully"() {
        given:
        GradleTaskRequest gradleTaskRequest = Mock(GradleTaskRequest)
        gradleTaskRequestFactory.create(_, _) >> gradleTaskRequest
        File ref = TestResource.resource(this, 'simplycd.json')
        File target = new File(codeDir, "build/simplycd.json")
        FileUtils.copyFile(ref, target)

        when:
        step.execute(build)

        then:
        1 * installationInfo.projectInstanceDir(build) >> codeDir
        1 * gradleTaskExecutor.execute(build, _) // Groovy cannot deal with TaskKey, so have to use wildcard
        1 * gradleTaskRequestFactory.create(build, _) >> this.gradleTaskRequest
    }
}
