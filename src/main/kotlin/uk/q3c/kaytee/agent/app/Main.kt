package uk.q3c.kaytee.agent.app

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.bval.guice.ValidationModule
import org.slf4j.LoggerFactory.getLogger
import ratpack.kotlin.handling.ratpack
import ru.vyarus.guice.persist.orient.OrientModule
import uk.q3c.build.gitplus.GitPlusModule
import uk.q3c.kaytee.agent.build.BuildModule
import uk.q3c.kaytee.agent.eventbus.GlobalBusModule
import uk.q3c.kaytee.agent.i18n.I18NModule
import uk.q3c.kaytee.agent.lifecycle.LifecycleModule
import uk.q3c.kaytee.agent.project.ProjectModule
import uk.q3c.kaytee.agent.queue.BuildRequestHandler
import uk.q3c.kaytee.agent.queue.QueueModule
import uk.q3c.kaytee.agent.system.BaseDirectoryReader
import uk.q3c.kaytee.agent.system.SystemModule
import uk.q3c.rest.hal.HalMapper


object Main {
    private val log = getLogger(Main::class.java)

    @JvmStatic fun main(args: Array<String>) {
        ratpack {

            serverConfig {
                development(System.getProperty(developmentMode_propertyName, "true").toBoolean())
                baseDir(BaseDirectoryReader.baseDir())
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
                module(OrientModule("memory:dbname", "admin", "admin"))
                bind(PersistenceService::class.java)
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


