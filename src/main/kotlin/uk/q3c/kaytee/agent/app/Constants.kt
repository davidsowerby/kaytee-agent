package uk.q3c.kaytee.agent.app

import java.time.OffsetDateTime

/**
 * Created by David Sowerby on 18 Feb 2017
 */

//val baseUrl = "http://localhost:9001"
//val errorBaseUrl = "$baseUrl/docs/errors"
val buildRequests = "buildRequests"
val subscriptions = "subscriptions"
val buildRecords = "buildRecords"
val hook = "hook"
val httpStatusCodeLookup = "https://httpstatuses.com"

val zeroDate: OffsetDateTime = OffsetDateTime.MIN.plusYears(1)
val idProperty = "uid"
val unitProperty = "unit"
val rangeProperty = "range"
val rangeFromProperty = "rangeFrom"

val baseDir_propertyName = "kaytee-basedir"
val developmentMode_propertyName = "kaytee-developmentmode"
val baseDirFolderName = "kaytee-data"
val defaultDevelopmentBaseDir = "/tmp/$baseDirFolderName"

val notSpecified: String = "not specified"

//fun href(resourcePath: String) = "$baseUrl/$resourcePath"

