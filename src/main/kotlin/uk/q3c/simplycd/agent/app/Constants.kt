package uk.q3c.simplycd.agent.app

/**
 * Created by David Sowerby on 18 Feb 2017
 */

val baseUrl = "http://localhost:5050/"
val buildRequests = "buildRequests"
val subscriptions = "subscriptions"
val hook = "hook"
val httpStatusCodeLookup = "https://httpstatuses.com"

fun href(resourcePath: String) = "$baseUrl$resourcePath"


