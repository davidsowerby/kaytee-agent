package uk.q3c.simplycd.agent.i18n.lib

import spock.lang.Specification

import static uk.q3c.simplycd.agent.i18n.lib.MessageFormatMode.*

/**
 * Created by David Sowerby on 06 Mar 2017
 */
class DefaultMessageFormatTest extends Specification {


    def "format is valid"() {
        given:
        String pattern = "This is a {1} pattern where the {0} can be in any {2}"
        Object[] arguments = ["parameters", "simple", "order"]

        when:
        String result = DefaultMessageFormat.INSTANCE.format(mode, pattern, arguments)

        then:
        result == "This is a simple pattern where the parameters can be in any order"

        where:
        mode             | _
        STRICT           | _
        STRICT_EXCEPTION | _
        LENIENT          | _
    }


    def "pattern with multiple parameters of same index"() {

        given:
        String pattern = "This is a {0} pattern where the same argument is {0} and has {1}, and {0} again"
        Object[] arguments = ["repeated", "an additional argument", "surplus"]

        when: "strict"
        String result = DefaultMessageFormat.INSTANCE.format(pattern, arguments)

        then: "return unmodified pattern"
        result == "This is a {0} pattern where the same argument is {0} and has {1}, and {0} again"

        when: "strict_exception"
        result = DefaultMessageFormat.INSTANCE.format(STRICT_EXCEPTION, pattern, arguments)

        then: "throw exception"
        thrown MessageFormatException

        when: "lenient"
        result = DefaultMessageFormat.INSTANCE.format(LENIENT, pattern, arguments)

        then:
        result == "This is a repeated pattern where the same argument is repeated and has an additional argument, and repeated again"
    }


    def "matching params and args, mixed order"() {

        given:
        String pattern = "This is a {1} pattern where the {0} can be in any {2}{3}"
        Object[] arguments = ["parameters", "simple", "order", " you like"]
        when:
        String result = DefaultMessageFormat.INSTANCE.format(mode, pattern, arguments)
        then:
        result == "This is a simple pattern where the parameters can be in any order you like"

        where:
        mode             | _
        STRICT           | _
        STRICT_EXCEPTION | _
        LENIENT          | _

    }


    def "single digit parameter indexes"() {

        given:
        String pattern = "This is a {1} pattern where the {0} can be in any {2}{3}{4}{5}{6}{7}{8}{9}"
        Object[] arguments = ["parameters", "simple", "order", "a", "b", "c", "d", "e", "f", "g"]
        when:
        String result = DefaultMessageFormat.INSTANCE.format(mode, pattern, arguments)
        then:
        result == "This is a simple pattern where the parameters can be in any orderabcdefg"

        where:
        mode             | _
        STRICT           | _
        STRICT_EXCEPTION | _
        LENIENT          | _
    }


    def "double digit parameter indexes"() {

        given:
        String pattern = "This is a {1} pattern where the {0} can be in any {2}{3}{4}{5}{6}{7}{8}{9}{10}"
        Object[] arguments = ["parameters", "simple", "order", "a", "b", "c", "d", "e", "f", "g", "h"]

        when:
        String result = DefaultMessageFormat.INSTANCE.format(mode, pattern, arguments)

        then:
        result == "This is a simple pattern where the parameters can be in any orderabcdefgh"

        where:
        mode             | _
        STRICT           | _
        STRICT_EXCEPTION | _
        LENIENT          | _

    }


    def "'parameter' at beginning, middle or end is escaped"() {

        given:
        String pattern = "\\{0}This is a {1} pattern \\{0} where the {0} can be in any {2} ignoring \\{3}"
        Object[] arguments = ["parameters", "simple", "order"]
        when:
        String result = DefaultMessageFormat.INSTANCE.format(mode, pattern, arguments)
        then:
        result == "{0}This is a simple pattern {0} where the parameters can be in any order ignoring {3}"

        where:
        mode             | _
        STRICT           | _
        STRICT_EXCEPTION | _
        LENIENT          | _


    }


    def "not enough arguments for all parameters"() {

        given:
        String pattern = "This is a {1} pattern where the {0} can be in any {2}"
        Object[] arguments = ["parameters", "simple"]

        when:
        String result = DefaultMessageFormat.INSTANCE.format(pattern, arguments)

        then:
        result == pattern

        when:
        result = DefaultMessageFormat.INSTANCE.format(STRICT_EXCEPTION, pattern, arguments)

        then:
        thrown MessageFormatException

        when:
        result = DefaultMessageFormat.INSTANCE.format(LENIENT, pattern, arguments)

        then:
        result == "This is a simple pattern where the parameters can be in any {??}"
    }

    def "parameters missing from the sequence (arguments unused)"() {

        given:
        String pattern = "This is a {1} pattern where the {0} can be in any {3}"
        Object[] arguments = ["parameters", "simple", "unused", "order"]

        when:
        String result = DefaultMessageFormat.INSTANCE.format(pattern, arguments)

        then:
        result == pattern

        when:
        result = DefaultMessageFormat.INSTANCE.format(STRICT_EXCEPTION, pattern, arguments)

        then:
        thrown MessageFormatException

        when:
        result = DefaultMessageFormat.INSTANCE.format(LENIENT, pattern, arguments)

        then:
        result == "This is a simple pattern where the parameters can be in any order"
    }

    def "parameter sequence does not start at 0, number of args same as number of params"() {
        given:
        String pattern = "This is a {2} pattern where the {1} can be in any {3}"
        Object[] arguments = ["parameters", "simple", "order"]

        when:
        String result = DefaultMessageFormat.INSTANCE.format(pattern, arguments)

        then:
        result == pattern

        when:
        result = DefaultMessageFormat.INSTANCE.format(STRICT_EXCEPTION, pattern, arguments)

        then:
        thrown MessageFormatException

        when:
        result = DefaultMessageFormat.INSTANCE.format(LENIENT, pattern, arguments)

        then:
        result == "This is a order pattern where the simple can be in any {??}"
    }

    def "parameter sequence missing an index,  number of args appears correct"() {
        given:
        String pattern = "This is a {1} pattern where the {0} can be in any {3}"
        Object[] arguments = ["parameters", "simple", "order"]

        when:
        String result = DefaultMessageFormat.INSTANCE.format(pattern, arguments)

        then:
        result == pattern

        when:
        result = DefaultMessageFormat.INSTANCE.format(STRICT_EXCEPTION, pattern, arguments)

        then:
        thrown MessageFormatException

        when:
        result = DefaultMessageFormat.INSTANCE.format(LENIENT, pattern, arguments)

        then:
        result == "This is a simple pattern where the parameters can be in any {??}"
    }


}