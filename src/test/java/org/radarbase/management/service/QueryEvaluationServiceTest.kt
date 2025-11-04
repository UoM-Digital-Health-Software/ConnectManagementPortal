package org.radarbase.management.service


import io.mockk.every
import io.mockk.mockkObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.radarbase.management.ManagementPortalTestApp
import org.radarbase.management.config.BasePostgresIntegrationTest
import org.radarbase.management.domain.*
import org.radarbase.management.domain.enumeration.*
import org.radarbase.management.repository.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.annotation.Transactional
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.random.Random

import org.mockito.kotlin.*
import org.radarbase.auth.authorization.RoleAuthority
import org.radarbase.management.domain.Role
import org.springframework.beans.factory.annotation.Value
import java.time.*

/**
 * Test class for the SubjectService class.
 *
 * @see SubjectService
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [ManagementPortalTestApp::class])
@Transactional
class QueryEvaluationServiceTest(
    @Autowired private val queryEValuationService: QueryEValuationService,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val queryLogicRepository: QueryLogicRepository,
    @Autowired private val queryGroupRepository: QueryGroupRepository,
    @Autowired private val queryEvaluationRepository: QueryEvaluationRepository,
    @Autowired private val subjectRepository: SubjectRepository,
    @Autowired private val queryParticipantContentRepository: QueryParticipantContentRepository ,
    @Autowired private val awsService: AWSService,







) : BasePostgresIntegrationTest() {
      lateinit var userData: UserData

    private var queryParticipantRepository : QueryParticipantRepository = mock()
    private var pdfSummaryRequestRepository : PdfSummaryRequestRepository = mock()
    private var queryContentService: QueryContentService = mock()

    lateinit var queryEValuationServiceMock: QueryEValuationService
    fun generateUserData(valueHeartRate: Double, valueSleep: Long, HRV: Long)  : UserData{
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        val heartRateData =
            (1L until 8L).map { daysAgo ->
                val day = today.minusDays(daysAgo).format(formatter)
                val value = valueHeartRate
                DataPoint(day, value.toDouble())
            }.reversed()


        val sleepData =   (1L until 8L).map { daysAgo ->
            val day = today.minusDays(daysAgo).format(formatter)
            val value = valueSleep
            DataPoint(day, value.toDouble())
        }.reversed()


        val HRV =   (1L until 8L ).map { daysAgo ->
            val day = today.minusDays(daysAgo).format(formatter)
            val value = valueSleep
            DataPoint(day, value.toDouble())
        }.reversed()

        return UserData(
            metrics = mapOf(
                "HEART_RATE" to heartRateData,
                "SLEEP_LENGTH" to sleepData,
                "HRV" to HRV)
        )
    }
    @BeforeEach
    fun initTest() {

        queryEValuationServiceMock = spy( QueryEValuationService(
            queryLogicRepository,
            queryContentService,
            queryGroupRepository,
            queryEvaluationRepository,
            subjectRepository,
            queryParticipantRepository,
            queryParticipantContentRepository,
            awsService,
            pdfSummaryRequestRepository
        ))
        userData = generateUserData(64.2,8, 50)
    }
    fun createQueryGroup(): QueryGroup {
        val user = userRepository.findAll()[0];

        val queryGroup = QueryGroup();
        queryGroup.name = "TestQueryGroup"
        queryGroup.description = "description"
        queryGroup.createdBy = user;
        queryGroup.createdDate = ZonedDateTime.now();

        return queryGroup
    }

    fun createQuery(queryGroup: QueryGroup?, physicalMetric: PhysicalMetric, queryOperator: ComparisonOperator, timeframe: QueryTimeFrame, value: String)  : Query {
        var query = Query();

        query.queryGroup = queryGroup
        query.field = physicalMetric.toString()
        query.operator = queryOperator
        query.value = value
        query.timeFrame = timeframe
        query.entity = "physical"

        return query
    }

    fun createQuery(queryGroup: QueryGroup?, metric: String, queryOperator: ComparisonOperator, timeframe: QueryTimeFrame, value: String)  : Query {
        var query = Query();

        query.queryGroup = queryGroup
        query.field = metric
        query.operator = queryOperator
        query.value = value
        query.timeFrame = timeframe
        query.entity = "QUESTIONNAIRE_HISTOGRAM"

        return query
    }

    fun createQueryLogic(queryGroup: QueryGroup?, type: QueryLogicType, logicOperator: QueryLogicOperator?, query: Query?, parentQueryLogic : QueryLogic? ) : QueryLogic {
        val queryLogic = QueryLogic() ;
        queryLogic.id =  Random.nextLong()
        queryLogic.queryGroup = queryGroup
        queryLogic.type = type
        queryLogic.logicOperator = logicOperator
        queryLogic.query = query;
        queryLogic.parent = parentQueryLogic

        return queryLogic
    }


    //64.2


//    GREATER_THAN_OR_EQUALS(">="),


    fun createUserData(numberOfDays: Int = 7) : MutableMap<String, DataSummaryCategory> {

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val today = LocalDate.now()

        var userData  : MutableMap<String, DataSummaryCategory> = mutableMapOf()

        val histograms = arrayOf(
            mutableMapOf("4-6" to 1),
            mutableMapOf("2-4" to 1),
            mutableMapOf("4-6" to 1),
            mutableMapOf("0-2" to 1),
            mutableMapOf("0-2" to 1),
            mutableMapOf("0-2" to 1),
            mutableMapOf("0-2" to 1)
            )

        for(i in 1 until  numberOfDays + 1 ) {
            val date = today.minusDays(i.toLong()).format(formatter)


            userData[date] =
                DataSummaryCategory(
                    physical = mutableMapOf(
                        "sleep_length" to 8.0,
                        "heart_rate" to 64.2,
                        "hrv" to 50.0),
                    questionnaire_total = 0.0,
                    questionnaire_slider = mutableMapOf(),
                    questionnaire_histogram = HistogramResponse(
                        sleep = histograms[i-1],
                        whereabouts = mutableMapOf(),
                        social = mutableMapOf()
                    )
                )

        }



        return userData
    }

    @Test
    @Transactional
    fun testEvaluateSingleConditionGreaterThan() {

        val userData = createUserData()

        val hrQueyr  = createQuery(null, PhysicalMetric.HEART_RATE, ComparisonOperator.GREATER_THAN, QueryTimeFrame.PAST_WEEK, "60");
        val queryLogic1 = createQueryLogic(null, QueryLogicType.CONDITION, null, hrQueyr, null );
        val result = queryEValuationService.evaluateSingleCondition(queryLogic1, userData) ;

        Assertions.assertTrue(result);

    }
    @Test
    @Transactional
    fun testEvaluateSingleConditionGreaterThanOrEquals() {
        val userData = createUserData()

        val hrQueyr  = createQuery(null, PhysicalMetric.HEART_RATE, ComparisonOperator.GREATER_THAN_OR_EQUALS, QueryTimeFrame.PAST_6_MONTH, "64.2");
        val queryLogic1 = createQueryLogic(null, QueryLogicType.CONDITION, null, hrQueyr, null );
        val result = queryEValuationService.evaluateSingleCondition(queryLogic1, userData) ;

        Assertions.assertTrue(result);

    }

    @Test
    @Transactional
    fun testEvaluateSingleConditionEquals() {
        val userData = createUserData()

        val hrQueyr  = createQuery(null, PhysicalMetric.HEART_RATE, ComparisonOperator.EQUALS, QueryTimeFrame.PAST_6_MONTH, "64.2");
        val queryLogic1 = createQueryLogic(null, QueryLogicType.CONDITION, null, hrQueyr, null );
        val result = queryEValuationService.evaluateSingleCondition(queryLogic1, userData) ;

        Assertions.assertTrue(result);

    }

    @Test
    @Transactional
    fun testEvaluateSingleConditionNotEqual() {
        val userData = createUserData()

        val hrQueyr  = createQuery(null, PhysicalMetric.HEART_RATE, ComparisonOperator.NOT_EQUALS, QueryTimeFrame.PAST_6_MONTH, "65.2");
        val queryLogic1 = createQueryLogic(null, QueryLogicType.CONDITION, null, hrQueyr, null );
        val result = queryEValuationService.evaluateSingleCondition(queryLogic1, userData) ;

        Assertions.assertTrue(result);

    }

    @Test
    @Transactional
    fun testEvaluateSingleConditionLessThan() {
        val userData = createUserData()

        val hrQueyr  = createQuery(null, PhysicalMetric.HEART_RATE, ComparisonOperator.LESS_THAN, QueryTimeFrame.PAST_6_MONTH, "64.3");
        val queryLogic1 = createQueryLogic(null, QueryLogicType.CONDITION, null, hrQueyr, null );
        val result = queryEValuationService.evaluateSingleCondition(queryLogic1, userData) ;

        Assertions.assertTrue(result);
    }

    @Test
    @Transactional
    fun testEvaluateSingleConditionLessThanOrEquals() {
        val userData = createUserData()

        val hrQueyr  = createQuery(null, PhysicalMetric.HEART_RATE, ComparisonOperator.LESS_THAN_OR_EQUALS, QueryTimeFrame.PAST_6_MONTH, "64.2");
        val queryLogic1 = createQueryLogic(null, QueryLogicType.CONDITION, null, hrQueyr, null );
        val result = queryEValuationService.evaluateSingleCondition(queryLogic1, userData) ;

        Assertions.assertTrue(result);
    }


    @Test
    @Transactional
    fun buildLogicTreeShouldReturnTopRootLogicGroup() {
        val hrQueyr  = createQuery(null, PhysicalMetric.HEART_RATE, ComparisonOperator.GREATER_THAN, QueryTimeFrame.PAST_6_MONTH, "60");
        val sleepQuery  = createQuery(null, PhysicalMetric.SLEEP_LENGTH, ComparisonOperator.GREATER_THAN, QueryTimeFrame.PAST_6_MONTH, "60");
        val hrvQuery = createQuery(null, PhysicalMetric.HRV, ComparisonOperator.GREATER_THAN, QueryTimeFrame.PAST_6_MONTH, "60");


        val rootLogicQuery = createQueryLogic(null, QueryLogicType.LOGIC, QueryLogicOperator.AND, null, null);
        val queryLogic1 = createQueryLogic(null, QueryLogicType.CONDITION, null, hrQueyr, rootLogicQuery );

        val innerRootLogicQuery = createQueryLogic(null, QueryLogicType.LOGIC, QueryLogicOperator.OR, null, rootLogicQuery);
        val sleepCondition = createQueryLogic(null, QueryLogicType.CONDITION, null, sleepQuery, innerRootLogicQuery );
        val hrvCondition = createQueryLogic(null, QueryLogicType.CONDITION, null, hrvQuery, innerRootLogicQuery );

        val list : List<QueryLogic> = listOf(
            rootLogicQuery,
            queryLogic1,
            innerRootLogicQuery,
            sleepCondition,
            hrvCondition
        )

        val result = queryEValuationService.buildLogicTree(list);

        Assertions.assertEquals(result?.id,rootLogicQuery.id)
    }



    @Test
    @Transactional
    fun testEvaluteQueryCondition() {
        val userData = createUserData()


        var queryList: Map<String, Query> = mapOf(
           "HR" to createQuery(null, PhysicalMetric.HEART_RATE, ComparisonOperator.GREATER_THAN, QueryTimeFrame.PAST_YEAR, "60"),
            "SLEEP" to createQuery(null, PhysicalMetric.SLEEP_LENGTH, ComparisonOperator.EQUALS, QueryTimeFrame.PAST_YEAR, "8"),
            "HRV" to createQuery(null, PhysicalMetric.HRV, ComparisonOperator.LESS_THAN, QueryTimeFrame.PAST_YEAR, "50")
        )


        var root =  getRoot(queryList, QueryLogicOperator.AND, QueryLogicOperator.AND)


        var result = queryEValuationService.evaluteQueryCondition(root!!, userData);

        // should be true
        Assertions.assertFalse(result);

         queryList  = mapOf(
            "HR" to createQuery(null, PhysicalMetric.HEART_RATE, ComparisonOperator.GREATER_THAN, QueryTimeFrame.PAST_6_MONTH, "60"),
            "SLEEP" to createQuery(null, PhysicalMetric.SLEEP_LENGTH, ComparisonOperator.EQUALS, QueryTimeFrame.PAST_6_MONTH, "8"),
            "HRV" to createQuery(null, PhysicalMetric.HRV, ComparisonOperator.GREATER_THAN, QueryTimeFrame.PAST_6_MONTH, "50")
        )

         root =  getRoot(queryList, QueryLogicOperator.AND, QueryLogicOperator.AND)
         result = queryEValuationService.evaluteQueryCondition(root!!, userData);

        // should be false
         Assertions.assertFalse(result);


        queryList  = mapOf(
            "HR" to createQuery(null, PhysicalMetric.HEART_RATE, ComparisonOperator.GREATER_THAN, QueryTimeFrame.PAST_6_MONTH, "60"),
            "SLEEP" to createQuery(null, PhysicalMetric.SLEEP_LENGTH, ComparisonOperator.EQUALS, QueryTimeFrame.PAST_6_MONTH, "8"),
            "HRV" to createQuery(null, PhysicalMetric.HRV, ComparisonOperator.EQUALS, QueryTimeFrame.PAST_6_MONTH, "50")
        )

        root =  getRoot(queryList, QueryLogicOperator.AND, QueryLogicOperator.OR)
        result = queryEValuationService.evaluteQueryCondition(root!!, userData);

        // should be false
        Assertions.assertTrue(result);

    }



    @Test
    @Transactional
    fun testHistogramEvaluationAsTrue() {
        val userData = createUserData()

        val hrQueyr  = createQuery(null,"SLEEP", ComparisonOperator.IS, QueryTimeFrame.PAST_WEEK, "0-2");
        val queryLogic1 = createQueryLogic(null, QueryLogicType.CONDITION, null, hrQueyr, null );
        val result = queryEValuationService.evaluateSingleCondition(queryLogic1, userData) ;

        Assertions.assertTrue(result)
    }

    @Test
    @Transactional
    fun testHistogramEvaluationAsFalse() {
        val userData = createUserData()

        val hrQueyr  = createQuery(null,"SLEEP", ComparisonOperator.IS, QueryTimeFrame.PAST_WEEK, "4-6");
        val queryLogic1 = createQueryLogic(null, QueryLogicType.CONDITION, null, hrQueyr, null );
        val result = queryEValuationService.evaluateSingleCondition(queryLogic1, userData) ;

        Assertions.assertFalse(result)
    }

    @Test
    @Transactional
    fun testHistogramEvaluationLessThan7DaysOfData() {
        val userData = createUserData(6)

        val hrQueyr  = createQuery(null,"SLEEP", ComparisonOperator.IS, QueryTimeFrame.PAST_WEEK, "0-2");
        val queryLogic1 = createQueryLogic(null, QueryLogicType.CONDITION, null, hrQueyr, null );
        val result = queryEValuationService.evaluateSingleCondition(queryLogic1, userData) ;

        Assertions.assertFalse(result)
    }


    @Test
    @Transactional
    fun testHistogramAggregation() {
        var histogramEvalData = mutableMapOf<String, Int>()
        val userData = createUserData()

        for (data in userData) {
            val date = data.key
            queryEValuationService.aggregateDataForHistogramEvaluation("sleep",date, userData,  histogramEvalData)
        }


        Assertions.assertEquals(histogramEvalData["4-6"], 2)
        Assertions.assertEquals(histogramEvalData["2-4"], 1)
        Assertions.assertEquals(histogramEvalData["0-2"], 4)

    }

    @Test
    @Transactional
    fun testEvaluateSingleConditionWithLessThan7DaysOfData() {
        val userData = createUserData(6)

        val hrQueyr  = createQuery(null, PhysicalMetric.HEART_RATE, ComparisonOperator.NOT_EQUALS, QueryTimeFrame.PAST_6_MONTH, "65.2");
        val queryLogic1 = createQueryLogic(null, QueryLogicType.CONDITION, null, hrQueyr, null );
        val result = queryEValuationService.evaluateSingleCondition(queryLogic1, userData) ;

        Assertions.assertFalse(result);

    }


    @Test
    @Transactional
    fun testExtractDatesToQuery() {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val dayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        var result = queryEValuationService.extractDatesToQuery(QueryTimeFrame.PAST_WEEK)

        Assertions.assertEquals(7, result.size)
        Assertions.assertEquals(yesterday.format(dayFormatter), result.last())

        result = queryEValuationService.extractDatesToQuery(QueryTimeFrame.PAST_MONTH)
        Assertions.assertEquals(today.minusMonths(1).format(dayFormatter), result.first())

        result = queryEValuationService.extractDatesToQuery(QueryTimeFrame.PAST_6_MONTH)
        Assertions.assertEquals(today.minusMonths(6).format(dayFormatter), result.first())

        result = queryEValuationService.extractDatesToQuery(QueryTimeFrame.PAST_YEAR)
        Assertions.assertEquals(today.minusYears(1).format(dayFormatter), result.first())
    }

    private fun createParticipant(id: Long = 1L, sendEmail: Boolean = true): Subject {
        val project = Project().apply { projectName = "Test Project" }

        val user = User()
        var role = Role(Authority(RoleAuthority.PARTICIPANT))
        role.project = project

        user.setLogin("user$id")
        user?.roles
            ?.firstOrNull { r -> r.authority?.name == RoleAuthority.PARTICIPANT.authority }
            ?.project


        user.roles = mutableSetOf(role)

        return Subject().apply {
            this.id = id
            this.user = user
        }
    }

    private fun createQueryParticipant(
        subject: Subject? = createParticipant(),
        queryGroup: QueryGroup? = QueryGroup().apply { id = 1L; name = "Group1" }
    ): QueryParticipant {
        return QueryParticipant().apply {
            this.subject = subject
            this.queryGroup = queryGroup
        }
    }

    @Test
    fun `evaluateQueries does nothing if hour is not 5`() {
        mockkObject(TimeUtils)
        every { TimeUtils.getCurrentTime(ZoneId.of("Europe/London")) } returns LocalTime.of(16, 0)

        queryEValuationServiceMock.evaluateQueries()

        verify(queryParticipantRepository, never()).findAll()
        verify(pdfSummaryRequestRepository, never()).findFirstBySubjectOrderByRequestedOnDesc(any())
        verify(queryContentService, never()).processCompletedQueriesForParticipant(any())
    }

    @Test
    fun `evaluateQueries skips participants with null subject or queryGroup`() {
        mockkObject(TimeUtils)
        every { TimeUtils.getCurrentTime(ZoneId.of("Europe/London")) } returns LocalTime.of(5, 0)

        val qp1 = createQueryParticipant(subject = null)
        val qp2 = createQueryParticipant(queryGroup = null)
        whenever(queryParticipantRepository.findAll()).thenReturn(listOf(qp1, qp2))

        queryEValuationServiceMock.evaluateQueries()

        verify(pdfSummaryRequestRepository, never()).findFirstBySubjectOrderByRequestedOnDesc(any())
        verify(queryContentService, never()).processCompletedQueriesForParticipant(any())
    }

    @Test
    fun `evaluateQueries processes participant with emailSent true`() {
        mockkObject(TimeUtils)
        every { TimeUtils.getCurrentTime(ZoneId.of("Europe/London")) } returns LocalTime.of(5, 0)

        val participant = createParticipant()
        val queryParticipant = createQueryParticipant(subject = participant)
        whenever(queryParticipantRepository.findAll()).thenReturn(listOf(queryParticipant))

        val pdfSummary = PdfSummaryRequest().apply { emailSent = true }
        whenever(pdfSummaryRequestRepository.findFirstBySubjectOrderByRequestedOnDesc(participant))
            .thenReturn(pdfSummary)
        val result = mutableMapOf("Group1" to true)
        doReturn(result).whenever(queryEValuationServiceMock).testLogicEvaluation(participant, participant.activeProject!!.projectName!!, null)
        doReturn(true).whenever(queryContentService).processCompletedQueriesForParticipant(participant.id!!)

        queryEValuationServiceMock.evaluateQueries()

        verify(queryEValuationServiceMock, times(1)).testLogicEvaluation(participant, participant.activeProject!!.projectName!!, null)
        verify(queryContentService, times(1)).processCompletedQueriesForParticipant(participant.id!!)
    }

    @Test
    fun `evaluateQueries skips participant if latest PDF summary emailSent is false`() {
        mockkObject(TimeUtils)
        every { TimeUtils.getCurrentTime(ZoneId.of("Europe/London")) } returns LocalTime.of(5, 0)

        val participant = createParticipant()
        val queryParticipant = createQueryParticipant(subject = participant)
        whenever(queryParticipantRepository.findAll()).thenReturn(listOf(queryParticipant))

        val pdfSummary = PdfSummaryRequest().apply { emailSent = false }
        whenever(pdfSummaryRequestRepository.findFirstBySubjectOrderByRequestedOnDesc(participant))
            .thenReturn(pdfSummary)

        queryEValuationServiceMock.evaluateQueries()

        verify(queryEValuationServiceMock, never()).testLogicEvaluation(any(), any(), any())
        verify(queryContentService, never()).processCompletedQueriesForParticipant(any())
    }

    private fun getRoot(listQueries:  Map<String, Query>, rootLogic: QueryLogicOperator, innerRootLogic: QueryLogicOperator): QueryLogic? {

        val rootLogicQuery = createQueryLogic(null, QueryLogicType.LOGIC, rootLogic, null, null);
        val queryLogic1 = createQueryLogic(null, QueryLogicType.CONDITION, null, listQueries["HR"], rootLogicQuery );

        val innerRootLogicQuery = createQueryLogic(null, QueryLogicType.LOGIC, innerRootLogic, null, rootLogicQuery);
        val sleepCondition = createQueryLogic(null, QueryLogicType.CONDITION, null, listQueries["SLEEP"], innerRootLogicQuery );
        val hrvCondition = createQueryLogic(null, QueryLogicType.CONDITION, null, listQueries["HRV"], innerRootLogicQuery );

        val list : List<QueryLogic> = listOf(
            rootLogicQuery,
            queryLogic1,
            innerRootLogicQuery,
            sleepCondition,
            hrvCondition
        )

        val root = queryEValuationService.buildLogicTree(list);

        return root;
    }


}
