package uk.q3c.kaytee.agent.app

import com.google.common.collect.ImmutableList
import uk.q3c.kaytee.plugin.TaskKey
import uk.q3c.kaytee.plugin.TaskKey.*
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

val standardLifecycle: List<TaskKey> = ImmutableList.of(Unit_Test, Integration_Test, Generate_Build_Info, Generate_Change_Log, Publish_to_Local, Functional_Test, Acceptance_Test, Merge_to_Master, Tag, Bintray_Upload, Production_Test)
val delegatedLifecycle: List<TaskKey> = ImmutableList.of(Custom)


