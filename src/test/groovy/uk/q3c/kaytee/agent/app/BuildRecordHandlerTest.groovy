package uk.q3c.kaytee.agent.app

import com.google.common.collect.ImmutableList
import ratpack.guice.BindingsImposition
import ratpack.impose.ImpositionsSpec
import ratpack.test.MainClassApplicationUnderTest
import uk.q3c.kaytee.agent.api.BuildRecordList
import uk.q3c.kaytee.agent.build.BuildRecord
import uk.q3c.kaytee.agent.build.BuildRecordService

import java.time.OffsetDateTime

import static org.apache.http.HttpStatus.SC_BAD_REQUEST
import static org.apache.http.HttpStatus.SC_OK
import static ratpack.http.HttpMethod.GET
import static uk.q3c.kaytee.agent.i18n.DeveloperErrorMessageKey.Unrecognised_Build_Record_Id

/**
 * Created by David Sowerby on 01 Apr 2017
 */
@SuppressWarnings("GroovyAssignabilityCheck")
class BuildRecordHandlerTest extends HandlerTest {

    BuildRecordService buildRecordService = Mock(BuildRecordService)
    Map<UUID, BuildRecord> records = Mock(Map)

    @Override
    protected MainClassApplicationUnderTest createAut() {
        return new MainClassApplicationUnderTest(Main.class) {
            @Override
            protected void addImpositions(ImpositionsSpec impositions) {
                impositions.add(
                        BindingsImposition.of {
                            it.bindInstance(BuildRecordService.class, buildRecordService)
                        })
            }
        }
    }

    def setup() {

        handler = new BuildRecordHandler(errorResponseBuilder, buildRecordService)
        supportedMethods = ImmutableList.of(GET)
        uri = ConstantsKt.buildRecords
    }

    def "request default list of build requests"() {

        when:
        ResponseCheck<BuildRecordList> getCheck = doGet(SC_OK, BuildRecordList)

        then:
        1 * buildRecordService.list("days", 3, 0) >> new BuildRecordList(ImmutableList.of())
        getCheck.allChecks()
        getCheck.result.records.isEmpty()
    }

    def "request specific list of build requests"() {

        when:
        ResponseCheck<BuildRecordList> getCheck = doGet(SC_OK, BuildRecordList, "unit=weeks;range=2;rangeFrom=1")

        then:
        1 * buildRecordService.list("weeks", 2, 1) >> new BuildRecordList(ImmutableList.of())
        getCheck.allChecks()
        getCheck.result.records.isEmpty()
    }


    def "request valid build record, returns correct info"() {
        given:
        UUID validId = UUID.randomUUID()
        BuildRecord buildRecord = new BuildRecord(validId, OffsetDateTime.now(), false)

        when:
        ResponseCheck<BuildRecord> responseCheck = doGet(SC_OK, BuildRecord, "${ConstantsKt.idProperty}=$validId")

        then:
        1 * buildRecordService.get(validId) >> buildRecord
        responseCheck.allChecks()
        responseCheck.result.uid == validId
    }

    def "request build record with invalid Id, returns error response"() {
        when:
        ResponseCheck<ErrorResponse> responseCheck = doGet(SC_BAD_REQUEST, ErrorResponse, "${ConstantsKt.idProperty}=xxx", Unrecognised_Build_Record_Id)

        then:
        responseCheck.allChecks()

    }
}
