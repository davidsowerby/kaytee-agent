package uk.q3c.simplycd.agent.app

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory.getLogger
import ratpack.kotlin.handling.ratpack
import uk.q3c.build.gitplus.GitPlusModule
import uk.q3c.rest.hal.HalMapper
import uk.q3c.simplycd.agent.api.ApiModule
import uk.q3c.simplycd.agent.api.RootHandler
import uk.q3c.simplycd.agent.build.BuildModule
import uk.q3c.simplycd.agent.i18n.I18NModule
import uk.q3c.simplycd.agent.lifecycle.LifecycleModule
import uk.q3c.simplycd.agent.project.ProjectModule
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
                for (module in KrailBindingCollator().modules()) {
                    module(module)
                }
                module(I18NModule())
                module(LifecycleModule())
                module(ProjectModule())
                module(QueueModule())
                module(SystemModule())
                module(GitPlusModule())
            }
            handlers {
                //                all{ LoggingHandler()}

                path("api", RootHandler::class.java)

                prefix("products") {
                    get("list") {
                        render("Product List")
                    }

                    get("get") {
                        render("Product Get")
                    }

                    get("search") {
                        render("Product Search")
                    }
                }
//                get("buildRequests/:id?") {
//                    val id = pathTokens.getOrElse("id", { "default" })
//                    render("returning buildRequests from GET $id")
//
//                }
                prefix("wigglies") {
                    get("/:id?") {
                        //http://localhost:9000/wigglies/345
                        val id = pathTokens.getOrElse("id", { "default" })
                        render("returning wigglies from GET $id")
                    }
                    post {
                        render("\nposting wigglie\n\n")
                    }

                }

                get("wiggly") {
                    render("wiggly beast")
                }

                all(RootHandler::class.java)
            }
        }
    }
}


