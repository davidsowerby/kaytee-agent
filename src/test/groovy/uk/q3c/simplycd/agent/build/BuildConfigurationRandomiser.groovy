package uk.q3c.simplycd.agent.build

import uk.q3c.simplycd.lifecycle.SimplyCDProjectExtension

/**
 * Created by David Sowerby on 30 Jan 2017
 */
class BuildConfigurationRandomiser {

    SimplyCDProjectExtension config

    BuildConfigurationRandomiser() {
        this.config = new SimplyCDProjectExtension()
        Random random = new Random()
        configureTest(config.unitTest, random)
        configureTest(config.integrationTest, random)
        configureTest(config.functionalTest, random)
        configureTest(config.acceptanceTest, random)
        configureTest(config.productionTest, random)
    }

    def configureTest(SimplyCDProjectExtension.GroupConfig config, Random random) {
        config.enabled = random.nextBoolean()
        if (config.enabled) {
            config.auto = random.nextBoolean()
            config.qualityGate = random.nextBoolean()
            config.manual = random.nextBoolean()
            if (!(config.auto || config.manual)) {  // if both false, make it auto
                config.auto = true
            }
            boolean external = false
            String externalRepoUrl = ""
            String externalRepoTask = "test"
        }
    }
}
