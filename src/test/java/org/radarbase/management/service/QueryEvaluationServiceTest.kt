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
import java.time.*
import org.slf4j.LoggerFactory

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
    @Autowired private val queryEvaluationRepository: QueryEvaluationRepository,
    @Autowired private val subjectRepository: SubjectRepository,
    @Autowired private val queryParticipantContentRepository: QueryParticipantContentRepository ,
    @Autowired private val awsService: AWSService,
) : BasePostgresIntegrationTest() {

    private val log = LoggerFactory.getLogger(QueryEvaluationServiceTest::class.java)

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

    fun createQuery(queryGroup: QueryGroup?, physicalMetric: PhysicalMetric, queryOperator: ComparisonOperator, timeframe: QueryTimeFrame, value: String, referenceType: QueryReferenceType? = null, rollingWindow: QueryTimeFrame? = null)  : Query {
        var query = Query();

        query.queryGroup = queryGroup
        query.field = physicalMetric.toString()
        query.operator = queryOperator
        query.value = value
        query.timeFrame = timeframe
        query.entity = "physical"

        query.rollingWindow = rollingWindow
        query.referenceType = referenceType

        return query
    }

    fun createQuery(queryGroup: QueryGroup?, metric: String, queryOperator: ComparisonOperator, timeframe: QueryTimeFrame, value: String, referenceType: QueryReferenceType? = null, rollingWindow: QueryTimeFrame? = null )  : Query {
        var query = Query();

        query.queryGroup = queryGroup
        query.field = metric
        query.operator = queryOperator
        query.value = value
        query.timeFrame = timeframe
        query.entity = "QUESTIONNAIRE_HISTOGRAM"

        query.rollingWindow = rollingWindow
        query.referenceType = referenceType

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


    fun createUserData(numberOfDays: Int = 7) : MutableMap<String, DataSummaryCategory> {

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val today = LocalDate.now()

        val userData  : MutableMap<String, DataSummaryCategory> = mutableMapOf()

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
                        sleep = histograms[(i % 6)],
                        whereabouts = mutableMapOf(),
                        social = mutableMapOf()
                    )
                )

        }

        return userData
    }

    fun createRandomUserData(numberOfDays: Int = 7, multiplier: Double ) : MutableMap<String, DataSummaryCategory> {
        val random = Random(1234L)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val today = LocalDate.now()

        val userData  : MutableMap<String, DataSummaryCategory> = mutableMapOf()

        val histograms = arrayOf(
            mutableMapOf("4-6" to 1),
            mutableMapOf("2-4" to 1),
            mutableMapOf("4-6" to 1),
            mutableMapOf("0-2" to 1),
            mutableMapOf("0-2" to 1),
            mutableMapOf("0-2" to 1),
            mutableMapOf("0-2" to 1)
        )

        val baseSleep = 8.0
        val baseHR = 64.0
        val baseHRV = 50.0


        for(i in 1 until  numberOfDays + 1 ) {
            val date = today.minusDays(i.toLong()).format(formatter)

            val hrMultiplier = if (i <= 7) multiplier else 1.0
            val heartRate = baseHR * hrMultiplier + (-0..1).random(random)

            userData[date] =
                DataSummaryCategory(
                    physical = mutableMapOf(
                        "sleep_length" to (baseSleep + (-1..1).random(random)).toDouble(), // small random fluctuation
                        "heart_rate" to heartRate,
                        "hrv" to (baseHRV + (-5..5).random(random)).toDouble()
                    ),
                    questionnaire_total = 0.0,
                    questionnaire_slider = mutableMapOf(),
                    questionnaire_histogram = HistogramResponse(
                        sleep = histograms[(i % 6)],
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

        val hrQueyr  = createQuery(null, PhysicalMetric.HEART_RATE, ComparisonOperator.GREATER_THAN_OR_EQUALS, QueryTimeFrame.PAST_WEEK, "64.2");
        val queryLogic1 = createQueryLogic(null, QueryLogicType.CONDITION, null, hrQueyr, null );
        val result = queryEValuationService.evaluateSingleCondition(queryLogic1, userData) ;

        Assertions.assertTrue(result);

    }

    @Test
    @Transactional
    fun testEvaluateSingleConditionEquals() {
        val userData = createUserData()

        val hrQueyr  = createQuery(null, PhysicalMetric.HEART_RATE, ComparisonOperator.EQUALS, QueryTimeFrame.PAST_WEEK, "64.2");
        val queryLogic1 = createQueryLogic(null, QueryLogicType.CONDITION, null, hrQueyr, null );
        val result = queryEValuationService.evaluateSingleCondition(queryLogic1, userData) ;

        Assertions.assertTrue(result);

    }

    @Test
    @Transactional
    fun testEvaluateSingleConditionNotEqual() {
        val userData = createUserData()

        val hrQueyr  = createQuery(null, PhysicalMetric.HEART_RATE, ComparisonOperator.NOT_EQUALS, QueryTimeFrame.PAST_WEEK, "65.2");
        val queryLogic1 = createQueryLogic(null, QueryLogicType.CONDITION, null, hrQueyr, null );
        val result = queryEValuationService.evaluateSingleCondition(queryLogic1, userData) ;

        Assertions.assertTrue(result);

    }

    @Test
    @Transactional
    fun testEvaluateSingleConditionLessThan() {
        val userData = createUserData()

        val hrQueyr  = createQuery(null, PhysicalMetric.HEART_RATE, ComparisonOperator.LESS_THAN, QueryTimeFrame.PAST_WEEK, "64.3");
        val queryLogic1 = createQueryLogic(null, QueryLogicType.CONDITION, null, hrQueyr, null );
        val result = queryEValuationService.evaluateSingleCondition(queryLogic1, userData) ;

        Assertions.assertTrue(result);
    }

    @Test
    @Transactional
    fun testEvaluateSingleConditionLessThanOrEquals() {
        val userData = createUserData()

        val hrQueyr  = createQuery(null, PhysicalMetric.HEART_RATE, ComparisonOperator.LESS_THAN_OR_EQUALS, QueryTimeFrame.PAST_WEEK, "64.2");
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
            "HR" to createQuery(null, PhysicalMetric.HEART_RATE, ComparisonOperator.GREATER_THAN, QueryTimeFrame.PAST_WEEK, "60"),
            "SLEEP" to createQuery(null, PhysicalMetric.SLEEP_LENGTH, ComparisonOperator.EQUALS, QueryTimeFrame.PAST_WEEK, "8"),
            "HRV" to createQuery(null, PhysicalMetric.HRV, ComparisonOperator.EQUALS, QueryTimeFrame.PAST_WEEK, "50")
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
        QueryEvaluationOptions.minimumExpectedData = 0.8
        val userData = createUserData(4)

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


        Assertions.assertEquals(2, histogramEvalData["4-6"])
        Assertions.assertEquals(2, histogramEvalData["2-4"])
        Assertions.assertEquals(3, histogramEvalData["0-2"])
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
    fun testEvaluateSingleConditionWithRollingAverageEqual() {
        val userData = createUserData(37)

        val hrQueyr  = createQuery(null, PhysicalMetric.HEART_RATE, ComparisonOperator.EQUALS, QueryTimeFrame.PAST_WEEK, "0", QueryReferenceType.ROLLING_AVG, QueryTimeFrame.PAST_MONTH)
        val queryLogic1 = createQueryLogic(null, QueryLogicType.CONDITION, null, hrQueyr, null );
        val result = queryEValuationService.evaluateSingleCondition(queryLogic1, userData)

        Assertions.assertTrue(result);

    }

    @Test
    @Transactional
    fun testEvaluateSingleConditionWithRollingAverageHigher() {
        val userData = createRandomUserData(37, multiplier = 1.04)

        val hrQueyr  = createQuery(null, PhysicalMetric.HEART_RATE, ComparisonOperator.GREATER_THAN, QueryTimeFrame.PAST_WEEK, "4", QueryReferenceType.ROLLING_AVG, QueryTimeFrame.PAST_MONTH)
        val queryLogic1 = createQueryLogic(null, QueryLogicType.CONDITION, null, hrQueyr, null );
        val result = queryEValuationService.evaluateSingleCondition(queryLogic1, userData)

        Assertions.assertTrue(result);

    }

    @Test
    @Transactional
    fun testEvaluateSingleConditionWithRollingAverageLower() {
        val userData = createRandomUserData(37, multiplier = 1.03)

        val hrQueyr  = createQuery(null, PhysicalMetric.HEART_RATE, ComparisonOperator.LESS_THAN, QueryTimeFrame.PAST_WEEK, "4", QueryReferenceType.ROLLING_AVG, QueryTimeFrame.PAST_MONTH)
        val queryLogic1 = createQueryLogic(null, QueryLogicType.CONDITION, null, hrQueyr, null );
        val result = queryEValuationService.evaluateSingleCondition(queryLogic1, userData)

        Assertions.assertTrue(result);

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
        Assertions.assertEquals(today.minusDays(30).format(dayFormatter), result.first())

        result = queryEValuationService.extractDatesToQuery(QueryTimeFrame.PAST_6_MONTH)
        Assertions.assertEquals(today.minusDays(180).format(dayFormatter), result.first())

        result = queryEValuationService.extractDatesToQuery(QueryTimeFrame.PAST_YEAR)
        Assertions.assertEquals(today.minusDays(365).format(dayFormatter), result.first())
    }


    @Test
    @Transactional
    fun testRollingAverageJustAboveThreshold() {
        val userData = createRandomUserData(37, multiplier = 1.04)
        val hrQuery = createQuery(
            null,
            PhysicalMetric.HEART_RATE,
            ComparisonOperator.GREATER_THAN,
            QueryTimeFrame.PAST_WEEK,
            "3.9",
            QueryReferenceType.ROLLING_AVG,
            QueryTimeFrame.PAST_MONTH
        )
        val queryLogic = createQueryLogic(null, QueryLogicType.CONDITION, null, hrQuery, null)
        val result = queryEValuationService.evaluateSingleCondition(queryLogic, userData)

        Assertions.assertTrue(result)
    }

    @Test
    @Transactional
    fun testRollingAverageJustBelowThreshold() {
        val userData = createRandomUserData(37, multiplier = 1.03)
        val hrQuery = createQuery(
            null,
            PhysicalMetric.HEART_RATE,
            ComparisonOperator.GREATER_THAN,
            QueryTimeFrame.PAST_WEEK,
            "3.5",
            QueryReferenceType.ROLLING_AVG,
            QueryTimeFrame.PAST_MONTH
        )
        val queryLogic = createQueryLogic(null, QueryLogicType.CONDITION, null, hrQuery, null)
        val result = queryEValuationService.evaluateSingleCondition(queryLogic, userData)

        Assertions.assertFalse(result)
    }


    @Test
    @Transactional
    fun testMultiMetricRollingAverage() {

        val userData = createRandomUserData(37, multiplier = 1.04)
        val hrQuery = createQuery(
            null,
            PhysicalMetric.HEART_RATE,
            ComparisonOperator.GREATER_THAN,
            QueryTimeFrame.PAST_WEEK,
            "3.9",
            QueryReferenceType.ROLLING_AVG,
            QueryTimeFrame.PAST_MONTH
        )
        val sleepQuery = createQuery(
            null,
            PhysicalMetric.SLEEP_LENGTH,
            ComparisonOperator.GREATER_THAN,
            QueryTimeFrame.PAST_WEEK,
            "0.1",
            QueryReferenceType.ROLLING_AVG,
            QueryTimeFrame.PAST_MONTH
        )

        val rootLogic = createQueryLogic(null, QueryLogicType.LOGIC, QueryLogicOperator.AND, null, null)
        val hrLogic = createQueryLogic(null, QueryLogicType.CONDITION, null, hrQuery, rootLogic)
        val sleepLogic = createQueryLogic(null, QueryLogicType.CONDITION, null, sleepQuery, rootLogic)

        val logicList = listOf(rootLogic, hrLogic, sleepLogic)
        val tree = queryEValuationService.buildLogicTree(logicList)
        val result = queryEValuationService.evaluteQueryCondition(tree!!, userData)

        Assertions.assertTrue(result)
    }


    @Test
    @Transactional
    fun testRollingAverageWithMissingData() {
        val userData = createUserData(4)
        val hrQuery = createQuery(
            null,
            PhysicalMetric.HEART_RATE,
            ComparisonOperator.GREATER_THAN,
            QueryTimeFrame.PAST_WEEK,
            "0",
            QueryReferenceType.ROLLING_AVG,
            QueryTimeFrame.PAST_MONTH
        )
        val queryLogic = createQueryLogic(null, QueryLogicType.CONDITION, null, hrQuery, null)
        val result = queryEValuationService.evaluateSingleCondition(queryLogic, userData)

        Assertions.assertFalse(result)
    }

    @Test
    @Transactional
    fun testHistogramCombination() {
        val userData = createUserData(7)
        val sleepQuery = createQuery(null,"SLEEP", ComparisonOperator.IS, QueryTimeFrame.PAST_WEEK, "0-2")
        val socialQuery = createQuery(null,"SOCIAL", ComparisonOperator.IS, QueryTimeFrame.PAST_WEEK, "0-2")

        val rootLogic = createQueryLogic(null, QueryLogicType.LOGIC, QueryLogicOperator.AND, null, null)
        val sleepLogic = createQueryLogic(null, QueryLogicType.CONDITION, null, sleepQuery, rootLogic)
        val socialLogic = createQueryLogic(null, QueryLogicType.CONDITION, null, socialQuery, rootLogic)

        val tree = queryEValuationService.buildLogicTree(listOf(rootLogic, sleepLogic, socialLogic))
        val result = queryEValuationService.evaluteQueryCondition(tree!!, userData)

        Assertions.assertFalse(result)
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
    @Transactional
    fun calculateRollingAvgTest() {
        val dayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        val userData = createUserData(4)

        val hrQuery  = createQuery(null,"SLEEP", ComparisonOperator.IS, QueryTimeFrame.PAST_WEEK, "0-2", QueryReferenceType.ROLLING_AVG, QueryTimeFrame.PAST_MONTH);
        val queryLogic1 = createQueryLogic(null, QueryLogicType.CONDITION, null, hrQuery, null );
        val result = queryEValuationService.getRollingAvgDates(hrQuery)


        val today = LocalDate.now()
        val endDate = today.minusWeeks(1)
        val startDate =  endDate.minusDays(30)



        Assertions.assertEquals( dayFormatter.format(startDate) ,result?.get(0) )
        Assertions.assertEquals( dayFormatter.format(endDate.minusDays(1)) ,result!!.get(result!!.size - 1) )


        Assertions.assertNotNull(result)


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
