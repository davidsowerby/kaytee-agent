package uk.q3c.simplycd.agent.app

import ratpack.server.PublicAddress
import java.net.URL

/**
 * Created by David Sowerby on 10 Apr 2017
 */
class SharedPublicAddress : PublicAddress {
    lateinit var url: URL
}