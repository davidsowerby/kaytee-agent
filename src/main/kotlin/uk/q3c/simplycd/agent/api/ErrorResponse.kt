package uk.q3c.simplycd.agent.api

import uk.q3c.rest.hal.HalResource

/**
 * Created by David Sowerby on 04 Mar 2017
 */
class ErrorResponse(val httpCode: Int, var detailCode: String, var userMessage: String, var developerMessage: String) : HalResource()