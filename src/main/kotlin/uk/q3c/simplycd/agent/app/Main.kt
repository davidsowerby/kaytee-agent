package uk.q3c.simplycd.agent.app

import org.slf4j.LoggerFactory.getLogger
import ratpack.handling.Context
import ratpack.server.BaseDir
import ratpack.server.RatpackServer

object Main {
    private val log = getLogger(Main::class.java)

    @JvmStatic fun main(args: Array<String>) {
        try {
            createServer().start()
        } catch (e: Exception) {
            log.error("", e)
            System.exit(1)
        }
    }

    fun createServer(): RatpackServer = serverOf {
        serverConfig {
            baseDir(BaseDir.find())
        }

        guiceRegistry {
            module(MyModule())
        }

        handlers {
            path("foo") { render("from the foo handler") }
            path("bar") { render("from the bar handler") }

            // Map to /baz using a Kotlin function
            path("baz", ::bazHandler)

            // Set up a nested routing block, which is delegated to `nestedHandler`
            prefix("nested") {
                path(":var1/:var2?") {
                    // The path tokens are the :var1 and :var2 path components above
                    val var1 = pathTokens["var1"]
                    val var2 = pathTokens["var2"]
                    render("from the nested handler, var1: $var1, var2: $var2")
                }
            }

            // Map to a dependency injected handler
            path("buildRequests", RootHandler::class.java)
//            path("", RootHandler::class.java)


            // Bind the /static app path to the src/ratpack/assets/images dir
            prefix("static") {
                fileSystem("assets/images") { files() }
            }
//            path("", RootHandler::class.java)
            get { render("GET") }
            put { render("PUT") }
            post { render("POST") }
            all { render("ALL") }
        }
    }
}

/** A handler as a method */
fun bazHandler(context: Context) = context.render("from the baz handler")
