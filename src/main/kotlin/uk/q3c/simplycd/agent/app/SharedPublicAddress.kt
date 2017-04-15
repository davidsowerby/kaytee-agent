package uk.q3c.simplycd.agent.app

import ratpack.http.HttpUrlBuilder
import ratpack.server.PublicAddress
import uk.q3c.simplycd.agent.i18n.DeveloperErrorMessageKey
import java.net.URI

/**
 * Created by David Sowerby on 10 Apr 2017
 */
class SharedPublicAddress : ExpandedPublicAddress {
    var uri: URI = URI("http://localhost")
    var port = 9001
    val errorBaseUrl = "docs/errors"

    override fun errorDocUrl(messageKey: DeveloperErrorMessageKey): URI {
        val errorUri = HttpUrlBuilder.base(uri).port(port).path("$errorBaseUrl/${segmentFromErrorKey(messageKey)}").build()
        return errorUri
    }

    private fun segmentFromErrorKey(key: DeveloperErrorMessageKey): String {
        return key.name.replace("_", "").decapitalize()
    }

    override fun get(): URI {
        return HttpUrlBuilder.base(uri).port(port).build()
    }

}

interface ExpandedPublicAddress : PublicAddress {

    fun errorDocUrl(messageKey: DeveloperErrorMessageKey): URI
}
