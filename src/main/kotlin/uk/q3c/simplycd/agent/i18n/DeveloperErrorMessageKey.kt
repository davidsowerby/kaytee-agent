package uk.q3c.simplycd.agent.i18n

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
    Invalid_Project_Name(HttpStatus.SC_BAD_REQUEST)


}
