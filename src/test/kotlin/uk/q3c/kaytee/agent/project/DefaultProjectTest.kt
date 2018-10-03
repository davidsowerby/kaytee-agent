package uk.q3c.kaytee.agent.project

import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import uk.q3c.build.gitplus.remote.ServiceProvider
import java.net.URI
import java.util.*

/**
 * Created by David Sowerby on 17 Sep 2018
 */
object DefaultProjectTest : Spek({

    given("deconstruction from URI") {

        given("username / project name") {

            on("constructed, derived properties values are correct") {
                val uuid = UUID.randomUUID()
                val project = DefaultProject(ServiceProvider.GITLAB, URI("https://gitlab.com/dsowerby/q3c-testutils"), uuid)

                it("should return correct property values") {
                    project.projectName.shouldBeEqualTo("q3c-testutils")
                    project.remoteNamespace.shouldBeEqualTo("dsowerby")
                }
            }
        }

        given("group / project name") {
            on("constructed, derived properties values are correct") {
                val uuid = UUID.randomUUID()
                val project = DefaultProject(ServiceProvider.GITLAB, URI("https://gitlab.com/kayman-group/club-account/club-account-ui"), uuid)
                it("should return correct property values") {
                    project.projectName.shouldBeEqualTo("club-account-ui")
                    project.remoteNamespace.shouldBeEqualTo("kayman-group/club-account")
                }

            }
        }
    }


})