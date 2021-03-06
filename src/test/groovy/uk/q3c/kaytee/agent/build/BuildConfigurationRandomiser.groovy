package uk.q3c.kaytee.agent.build

import uk.q3c.kaytee.plugin.GroupConfig
import uk.q3c.kaytee.plugin.KayTeeExtension
import uk.q3c.kaytee.plugin.TaskType

/**
 * Created by David Sowerby on 30 Jan 2017
 */
class BuildConfigurationRandomiser {

    KayTeeExtension config
    int delegated = 0

    BuildConfigurationRandomiser() {
        this.config = new KayTeeExtension()
        Random random = new Random()
        configureTest(config.unitTest, random)
        configureTest(config.integrationTest, random)
        configureTest(config.functionalTest, random)
        configureTest(config.acceptanceTest, random)
        configureTest(config.productionTest, random)
    }

    def configureTest(GroupConfig config, Random random) {
        config.enabled = random.nextBoolean()
        if (config.enabled) {
            int select = random.nextInt(2)
            switch (select) {
                case 0: config.taskType = TaskType.GRADLE; break
                case 1: config.taskType = TaskType.DELEGATED; break
                case 2: config.taskType = TaskType.MANUAL
            }
            config.qualityGate = random.nextBoolean()
            if (config.isDelegated()) {
                config.delegate.repoUserName = 'davidsowerby'
                config.delegate.repoName = 'scratch'
                config.delegate.commitId = 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa'
                delegated++
            }
//            config.manual = random.nextBoolean()
//            if (!(config.auto || config.manual)) {  // if both false, make it auto
//                config.auto = true
//            }
        }
    }

}
