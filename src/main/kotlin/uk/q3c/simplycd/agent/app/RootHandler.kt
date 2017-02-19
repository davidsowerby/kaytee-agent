package uk.q3c.simplycd.agent.app

import ratpack.handling.Context
import ratpack.handling.Handler
import ratpack.jackson.Jackson
import uk.q3c.rest.ion.DefaultIonObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A handler implementation that is created via dependency injection.
 *
 * @see MyModule
 */
@Singleton class RootHandler @Inject constructor(val myService: MyService) : Handler {
    override fun handle(context: Context) {
        // build the API entry point response - the 'home page'
        val ionObject = DefaultIonObject(href = href(""))
        ionObject.addRelation(relationType = "buildRequests", relationIri = href("buildRequests"))
        return context.render(Jackson.json(ionObject))
    }
}
