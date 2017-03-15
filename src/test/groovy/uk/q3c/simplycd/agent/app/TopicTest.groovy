package uk.q3c.simplycd.agent.app

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Created by David Sowerby on 14 Mar 2017
 */
class TopicTest extends Specification {

    @Shared
    URL base = new URL("http://example.com")
    @Shared
    URL base1 = new URL("http://example.com/")
    @Shared
    URL base11 = new URL("http://example.com/builds")
    @Shared
    URL base111 = new URL("http://example.com/builds/1")
    @Shared
    URL base112 = new URL("http://example.com/builds/2")

    @Unroll
    "match all"() {

        when:
        Topic hookTopic = new Topic(topic)

        then:
        hookTopic.matches(new Topic(candidate)) == matches

        where:
        topic   | candidate || matches
        base    | base      || true
        base    | base1     || true
        base    | base11    || true
        base    | base111   || true
        base    | base112   || true
        base11  | base      || false
        base11  | base1     || false
        base11  | base11    || true
        base11  | base111   || true
        base11  | base112   || true
        base111 | base112   || false


    }
}
