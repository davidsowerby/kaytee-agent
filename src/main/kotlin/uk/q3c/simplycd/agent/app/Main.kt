package uk.q3c.simplycd.agent.app

import org.slf4j.LoggerFactory.getLogger
import ratpack.kotlin.handling.ratpack
import uk.q3c.simplycd.agent.build.BuildModule


object Main {
    private val log = getLogger(Main::class.java)

    @JvmStatic fun main(args: Array<String>) {
        ratpack {
            serverConfig {
                development(true)
                port(9000)
            }
            bindings {
                BuildModule()
            }
            handlers {
                get("buildRequests") {
                    render("hello ")
                }

                get("wiggly") {
                    render("wiggly beast")
                }

                get {
                    render("root")
                }
            }
        }
    }
}

