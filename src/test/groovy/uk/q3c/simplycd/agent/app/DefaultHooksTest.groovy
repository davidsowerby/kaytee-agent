package uk.q3c.simplycd.agent.app

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import spock.lang.Specification
import uk.q3c.simplycd.agent.build.BuildRecord

import java.time.OffsetDateTime
/**
 * Created by David Sowerby on 13 Mar 2017
 */
class DefaultHooksTest extends Specification {

    SubscriberNotifier hookNotifier = Mock(SubscriberNotifier)

    UUID uid1 = UUID.randomUUID()
    UUID uid2 = UUID.randomUUID()
    UUID uid3 = UUID.randomUUID()

    DefaultHooks hooks
    SharedPublicAddress publicAddress = new SharedPublicAddress()
    URL topic0
    URL topic1
    URL topic2
    URL topic3

    URL hookUrl1 = new URL("https://example.com/hook/1")
    URL hookUrl2 = new URL("https://example.com/hook/2")
    URL hookUrl3 = new URL("https://example.com/hook/3")

    Subscriber hook1 = new Subscriber(hookUrl1)
    Subscriber hook2 = new Subscriber(hookUrl2)
    Subscriber hook3 = new Subscriber(hookUrl3)
    BuildRecord msg1
    BuildRecord msg2
    BuildRecord msg3


    void setup() {
        msg1 = new BuildRecord(uid1, OffsetDateTime.now())
        msg2 = new BuildRecord(uid2, OffsetDateTime.now())
        msg3 = new BuildRecord(uid3, OffsetDateTime.now())
        topic0 = new URL(publicAddress.get("build").toString())
        topic1 = new URL(publicAddress.get(msg1.self().href).toString())
        topic2 = new URL(publicAddress.get(msg2.self().href).toString())
        topic3 = new URL(publicAddress.get(msg3.self().href).toString())
        hooks = new DefaultHooks(hookNotifier, publicAddress)
    }

    def "publish, subscribe and remove"() {


        when:
        def registered = hooks.registerTopic(topic1)

        then:
        registered

        when:
        def subscribed = hooks.subscribe(topic1, hookUrl1)

        then:
        subscribed


        when:
        hooks.publish(msg1)

        then:
        1 * hookNotifier.notify(ImmutableSet.of(hook1), msg1)

        when:
        hooks.unsubscribe(topic1, hookUrl1)
        hooks.publish(msg1)

        then:
        0 * hookNotifier.notify(ImmutableSet.of(hook1), msg1)

        when:
        hooks.registerTopic(topic0)
        hooks.registerTopic(topic2)
        hooks.subscribe(topic1, hookUrl1)
        hooks.subscribe(topic2, hookUrl2)
        hooks.subscribe(topic0, hookUrl3)
        hooks.publish(msg1)
        hooks.publish(msg2)

        then:
        1 * hookNotifier.notify(ImmutableSet.of(hook1, hook3), msg1)
        1 * hookNotifier.notify(ImmutableSet.of(hook2, hook3), msg2)
        0 * hookNotifier.notify(_, _)

        when:
        hooks.registerTopic(topic3)
        hooks.removeSubscriber(hookUrl1)
        hooks.publish(msg1)
        hooks.publish(msg2)


        then:
        1 * hookNotifier.notify(ImmutableSet.of(hook3), msg1)
        1 * hookNotifier.notify(ImmutableSet.of(hook2, hook3), msg2)
        0 * hookNotifier.notify(_, _)

        when: "2 subscribers added, one is a duplicate, but should not cause duplicate notification"
        subscribed = hooks.subscribe(topic1, ImmutableList.of(hookUrl2, hookUrl3))

        then:
        subscribed

        when:
        hooks.publish(msg1)
        hooks.publish(msg2)

        then:
        1 * hookNotifier.notify(ImmutableSet.of(hook2, hook3), msg1)
        1 * hookNotifier.notify(ImmutableSet.of(hook2, hook3), msg2)
        0 * hookNotifier.notify(_, _)

        when: "topic removed"
        def removed = hooks.removeTopic(topic2)

        then:
        removed

        when:
        hooks.publish(msg1)
        hooks.publish(msg2)

        then: "hook3 will get both messages because it subscribed to topic0 (the topic 'root')"
        1 * hookNotifier.notify(ImmutableSet.of(hook2, hook3), msg1)
        1 * hookNotifier.notify(ImmutableSet.of(hook3), msg2)
        0 * hookNotifier.notify(_, _)

        when:
        hooks.removeSubscriber(hookUrl3)
        hooks.publish(msg1)
        hooks.publish(msg2)

        then:
        0 * hookNotifier.notify(ImmutableSet.of(hook1), msg1)
        1 * hookNotifier.notify(ImmutableSet.of(hook2), msg1)
        0 * hookNotifier.notify(ImmutableSet.of(hook3), msg1)

        0 * hookNotifier.notify(ImmutableSet.of(hook1), msg2)
        0 * hookNotifier.notify(ImmutableSet.of(hook2), msg2)
        0 * hookNotifier.notify(ImmutableSet.of(hook3), msg2)

    }

    def "subscribe to non-existent topic returns false"() {

        expect:
        !hooks.subscribe(topic1, hookUrl1)
        !hooks.subscribe(topic1, ImmutableList.of(hookUrl2, hookUrl3))
    }

    def "Register already registered topic returns false"() {

        expect:
        hooks.registerTopic(topic1)
        !hooks.registerTopic(topic1)
    }

}
