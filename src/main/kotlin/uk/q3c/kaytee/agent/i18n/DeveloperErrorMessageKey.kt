package uk.q3c.kaytee.agent.i18n

/**
 * Created by David Sowerby on 03 May 2016 [DeveloperErrorMessages]
 *
 * @see DeveloperErrorMessages
 */

import org.apache.http.HttpStatus
import org.apache.http.HttpStatus.SC_METHOD_NOT_ALLOWED
import uk.q3c.krail.core.i18n.I18NKey

enum class DeveloperErrorMessageKey(val httpCode: Int) : I18NKey {

    Invalid_Method(SC_METHOD_NOT_ALLOWED),
    Invalid_Project_Name(HttpStatus.SC_BAD_REQUEST),
    Invalid_Topic(HttpStatus.SC_BAD_REQUEST),
    Exception_in_Handler(HttpStatus.SC_INTERNAL_SERVER_ERROR),

    Unrecognised_Build_Record_Id(HttpStatus.SC_BAD_REQUEST)

}
