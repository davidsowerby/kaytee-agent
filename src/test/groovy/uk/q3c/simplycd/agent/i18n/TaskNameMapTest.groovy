package uk.q3c.simplycd.agent.i18n

import spock.lang.Specification

/**
 * Created by David Sowerby on 25 Apr 2017
 */
class TaskNameMapTest extends Specification {

    TaskNameMap map

    def setup() {
        map = new TaskNameMap()
    }


    def "quality gate lookup"() {
        expect:
        map.get(TaskKey.Unit_Test, true) == "testQualityGate"
        map.get(TaskKey.Integration_Test, true) == "integrationTestQualityGate"
        map.get(TaskKey.Functional_Test, true) == "functionalTestQualityGate"
        map.get(TaskKey.Acceptance_Test, true) == "acceptanceTestQualityGate"
        map.get(TaskKey.Production_Test, true) == "productionTestQualityGate"

        map.get(TaskKey.Unit_Test) == "test"
        map.get(TaskKey.Integration_Test) == "clean integrationTest"
        map.get(TaskKey.Functional_Test) == "functionalTest"
        map.get(TaskKey.Acceptance_Test) == "acceptanceTest"
        map.get(TaskKey.Production_Test) == "productionTest"
    }
}
