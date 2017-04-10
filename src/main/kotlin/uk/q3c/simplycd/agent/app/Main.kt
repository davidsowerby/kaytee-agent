package uk.q3c.simplycd.agent.app

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.bval.guice.ValidationModule
import org.slf4j.LoggerFactory.getLogger
import ratpack.kotlin.handling.ratpack
import uk.q3c.build.gitplus.GitPlusModule
import uk.q3c.rest.hal.HalMapper
import uk.q3c.simplycd.agent.build.BuildModule
import uk.q3c.simplycd.agent.eventbus.GlobalBusModule
import uk.q3c.simplycd.agent.i18n.I18NModule
import uk.q3c.simplycd.agent.lifecycle.LifecycleModule
import uk.q3c.simplycd.agent.project.ProjectModule
import uk.q3c.simplycd.agent.queue.BuildRequestHandler
import uk.q3c.simplycd.agent.queue.QueueModule
import uk.q3c.simplycd.agent.system.SystemModule


object Main {
    private val log = getLogger(Main::class.java)

    @JvmStatic fun main(args: Array<String>) {
        ratpack {
            serverConfig {
                development(true)
                port(9000)
            }
            bindings {
                add(ObjectMapper::class.java, HalMapper())
                module(BuildModule())
                module(ApiModule())
                module(GlobalBusModule())
                module(I18NModule())
                module(LifecycleModule())
                module(ProjectModule())
                module(QueueModule())
                module(SystemModule())
                module(GitPlusModule())
                module(ValidationModule())
            }
            handlers {
                path(subscriptions, SubscriptionHandler::class.java)
                path(buildRequests, BuildRequestHandler::class.java)
                path(buildRecords, BuildRecordHandler::class.java)
                path(hook, SubscriptionHandler::class.java)
                all(RootHandler::class.java)
            }
        }
    }
}


