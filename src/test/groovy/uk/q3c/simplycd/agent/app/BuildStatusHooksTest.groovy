package uk.q3c.simplycd.agent.app

import com.google.common.collect.ImmutableList
import spock.lang.Specification

/**
 * Created by David Sowerby on 13 Mar 2017
 */
class BuildStatusHooksTest extends Specification {

    HookNotifier hookNotifier = Mock(HookNotifier)

    UUID uid1 = UUID.randomUUID()
    UUID uid2 = UUID.randomUUID()
    UUID uid3 = UUID.randomUUID()

    BuildStatusHooks hooks
    URL topic1 = new URL(ConstantsKt.href("build/$uid1"))
    URL topic2 = new URL(ConstantsKt.href("build/$uid2"))
    URL topic3 = new URL(ConstantsKt.href("build/$uid3"))

    URL hookUrl1 = new URL("https://example.com/hook/1")
    URL hookUrl2 = new URL("https://example.com/hook/2")
    URL hookUrl3 = new URL("https://example.com/hook/3")

    HookCallback hook1 = new HookCallback(hookUrl1)
    HookCallback hook2 = new HookCallback(hookUrl2)
    HookCallback hook3 = new HookCallback(hookUrl3)


    void setup() {
        hooks = new BuildStatusHooks(hookNotifier)
    }

    def "publish, subscribe and remove"() {
        given:
        BuildStatusMessage msg1 = new BuildStatusMessage(uid1)
        BuildStatusMessage msg2 = new BuildStatusMessage(uid2)
        hooks.subscribe(topic1, hookUrl1)

        when:
        hooks.publish(msg1)

        then:
        1 * hookNotifier.notify(hook1, msg1)

        when:
        hooks.unsubscribe(topic1, hookUrl1)
        hooks.publish(msg1)

        then:
        0 * hookNotifier.notify(hook1, msg1)

        when:
        hooks.subscribe(topic1, hookUrl1)
        hooks.subscribe(topic2, hookUrl2)
        hooks.subscribeToAllTopics(hookUrl3)
        hooks.publish(msg1)
        hooks.publish(msg2)

        then:
        1 * hookNotifier.notify(hook1, msg1)
        0 * hookNotifier.notify(hook2, msg1)
        1 * hookNotifier.notify(hook3, msg1)

        0 * hookNotifier.notify(hook1, msg2)
        1 * hookNotifier.notify(hook2, msg2)
        1 * hookNotifier.notify(hook3, msg2)

        when:
        hooks.unsubscribeFromAll(hookUrl1)
        hooks.publish(msg1)
        hooks.publish(msg2)


        then:
        0 * hookNotifier.notify(hook1, msg1)
        0 * hookNotifier.notify(hook2, msg1)
        1 * hookNotifier.notify(hook3, msg1)

        0 * hookNotifier.notify(hook1, msg2)
        1 * hookNotifier.notify(hook2, msg2)
        1 * hookNotifier.notify(hook3, msg2)

        when: "2 subscribers added, one is a duplicate, but should not cause duplicate notification"
        hooks.subscribe(topic1, ImmutableList.of(hookUrl2, hookUrl3))
        hooks.publish(msg1)
        hooks.publish(msg2)

        then:
        0 * hookNotifier.notify(hook1, msg1)
        1 * hookNotifier.notify(hook2, msg1)
        1 * hookNotifier.notify(hook3, msg1)

        0 * hookNotifier.notify(hook1, msg2)
        1 * hookNotifier.notify(hook2, msg2)
        1 * hookNotifier.notify(hook3, msg2)

        when: "topic removed"
        hooks.removeTopic(topic2)
        hooks.publish(msg1)
        hooks.publish(msg2)

        then:
        0 * hookNotifier.notify(hook1, msg1)
        1 * hookNotifier.notify(hook2, msg1)
        1 * hookNotifier.notify(hook3, msg1)

        0 * hookNotifier.notify(hook1, msg2)
        0 * hookNotifier.notify(hook2, msg2)
        0 * hookNotifier.notify(hook3, msg2)

    }

    def "Add1"() {

        expect: false
    }

    def "Remove"() {

        expect: false
    }

    def "RemoveAll"() {

        expect: false
    }
}
