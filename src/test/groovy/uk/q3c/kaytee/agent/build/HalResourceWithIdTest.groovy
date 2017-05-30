package uk.q3c.kaytee.agent.build

import spock.lang.Specification

/**
 * Created by David Sowerby on 10 Apr 2017
 */
class HalResourceWithIdTest extends Specification {

    def setup() {

    }

    def "'self' is set from id "() {
        given:
        UUID uid = UUID.randomUUID()

        when:
        HalResourceWithId resource = new HalResourceWithId(uid, "buildRequests")

        then:
        resource.self().href == "buildRequests/?uid=$uid".toString()
    }
}
