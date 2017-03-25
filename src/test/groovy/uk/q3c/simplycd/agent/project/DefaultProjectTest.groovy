package uk.q3c.simplycd.agent.project

import spock.lang.Specification

/**
 * Created by David Sowerby on 21 Mar 2017
 */
class DefaultProjectTest extends Specification {

    def "split out user and project name"() {

        given:
        UUID uid = UUID.randomUUID()
        Project project = new DefaultProject("davidsowerby/q3c-testUtil", uid)

        expect:
        project.fullProjectName == "davidsowerby/q3c-testUtil"
        project.remoteUserName == "davidsowerby"
        project.shortProjectName == "q3c-testUtil"
        project.uid == uid
    }
}
