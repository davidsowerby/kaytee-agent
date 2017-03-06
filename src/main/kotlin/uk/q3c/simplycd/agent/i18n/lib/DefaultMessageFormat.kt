package uk.q3c.simplycd.agent.i18n.lib

import org.slf4j.LoggerFactory
import org.slf4j.helpers.MessageFormatter
import uk.q3c.simplycd.agent.i18n.lib.MessageFormatMode.STRICT
import uk.q3c.simplycd.agent.i18n.lib.MessageFormatMode.STRICT_EXCEPTION
import java.util.*

/**
 * The native Java {MessageFormat] class exhibits some very strange behaviour when dealing with a single apostrophe.  The Javadoc for it even includes this warning:
 *
 * > **Warning:**
 * > The rules for using quotes within message format patterns unfortunately have shown to be somewhat confusing. In particular, it isn't always obvious to localizers whether single quotes need to be doubled or not. Make sure to inform localizers about the rules, and tell them (for example, by using comments in resource bundle source files) which strings will be processed by MessageFormat. Note that localizers may need to use single quotes in translated strings where the original version doesn't have them.
 *
 * This makes the native Java class very messy to use with I18N, or indeed anything which requires the use of a single apostrophe.
 *
 * The [MessageFormatter] from slf4j is fast, incredibly well used and does not suffer from the apostrophe problem.  However, it is designed for logging, and therefore does not allow for parameters being in a different order for different languages
 *
 * This implementation uses [MessageFormatter] to do the hard work, and sorts the provided arguments into the correct order first, enabling its use in an I18N environment
 *
 *  To assist this, the pattern contains prameter placeholders with index numbers, for example, a pattern may look like:
 *
 * > *this is a {1} pattern where the {0} can be in any {2}*
 *
 * which when provided with arguments:
 *
 * > *"parameters", "simple", "order"*
 *
 * will result in
 *
 * > *this is a simple pattern where the parameters can be in any order*
 *
 * No claims are made for efficiency or performance - [MessageFormatter] is fast, but this utility has not been optimised.
 *
 * If you want to include a "{" in the output, simply escape it "\\{". This will escape the whole placeholder
 *
 * There are 3 modes of operation, although two are just variants of the strict mode
 *
 *  # [MessageFormatMode.STRICT] mode:
 * You can have any number of parameters, provided:
 *  1. the parameter index sequence is continuous,
 *  1. the parameter index sequence starts from zero,
 *  1. there are no duplicated parameter indexes numbers
 *  1. the number of arguments is exactly the same as the number of parameters
 *
 * If any of these conditions are not met, the pattern is returned unmodified
 *
 *  # [MessageFormatMode.STRICT_EXCEPTION] mode:
 *
 *  Identical to [MessageFormatMode.STRICT] except that a [MessageFormatException] is thrown if conditions are not met
 *
 *  # [MessageFormatMode.LENIENT] mode
 *
 *  1.  There may be index numbers missing from the parameter sequence
 *  2.  A parameter index may be repeated within a pattern
 *  3.  There need not be the same number of arguments as parameters
 *
 *  Unused arguments are just ignored.
 *
 *  Parameters without matching arguments are assigned a value of "??"
 */
object StrictMessageFormat {
    private val log = LoggerFactory.getLogger(StrictMessageFormat::class.java)


    @JvmOverloads
    fun format(mode: MessageFormatMode = STRICT, pattern: String, vararg arguments: Any): String {
        val parameters = ArrayList<Int>()
        val strippedPattern = scanForParameters(pattern, parameters)

        when (mode) {
            STRICT -> if (!argsMatchParams(parameters, arguments)) {
                log.warn(strictFailureMessage(parameters, arguments, pattern))
                return pattern
            }
            STRICT_EXCEPTION -> if (!argsMatchParams(parameters, arguments)) {
                throw MessageFormatException(strictFailureMessage(parameters, arguments, pattern))
            }
            else -> {
                // not strict, no checks
            }
        }

        val sortedArguments = sortArguments(parameters, arguments)
        return MessageFormatter.arrayFormat(strippedPattern, sortedArguments)
                .message

    }

    private fun strictFailureMessage(parameters: ArrayList<Int>, arguments: Array<out Any>, pattern: String): String {
        return "Message pattern and arguments do not match, there are ${parameters.size} parameters in the pattern, and ${arguments.size} arguments. The pattern is: '$pattern'.  LENIENT mode would allow this"
    }

    private fun scanForParameters(pattern: String, parameters: MutableList<Int>): String {
        var i = 0
        val strippedPattern = StringBuilder()
        while (i < pattern.length) {
            var c = pattern[i]
            // if the '{' has been escaped this moves the scan beyond it, thereby ignoring it
            if (c == '\\') {
                i++
                c = pattern[i]
                if (c == '{') {
                    strippedPattern.append('{')
                    i++
                    c = pattern[i]
                    strippedPattern.append(c)
                }
            } else {
                strippedPattern.append(c)
            }
            // find an opening brace
            if (c == '{') {
                // find the closing '}' and extract
                val placeholder = StringBuilder()
                var done = false

                while (!done) {
                    i++
                    c = pattern[i]

                    if (c == '}') {
                        parameters.add(Integer.valueOf(placeholder.toString()))
                        strippedPattern.append(c)
                        done = true
                    } else {
                        placeholder.append(c)
                    }
                }
            }

            i++
        }
        return strippedPattern.toString()
    }

    private fun argsMatchParams(parameters: List<Int>, arguments: Array<out Any>): Boolean {
        // strict requires matching numbers of params and args
        if (parameters.size != arguments.size) {
            return false
        }
        // strict requires that param indexes are a complete sequence
        for (i in 0..parameters.size - 1) {
            if (!parameters.contains(i)) {
                return false
            }
        }
        return true
    }


    private fun sortArguments(parameters: List<Int>, arguments: Array<out Any>): Array<Any> {
        val sortedArguments = ArrayList<Any>()
        for (i in parameters) {
            val argument = if (i < arguments.size) {
                arguments[i]
            } else {
                "{??}"
            }
            sortedArguments.add(argument)
        }
        return sortedArguments.toTypedArray()
    }
}

class MessageFormatException(msg: String) : RuntimeException(msg)

enum class MessageFormatMode {
    STRICT, STRICT_EXCEPTION, LENIENT
}
