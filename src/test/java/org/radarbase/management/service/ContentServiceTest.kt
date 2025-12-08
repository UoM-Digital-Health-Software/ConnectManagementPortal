package org.radarbase.management.service


import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.radarbase.management.ManagementPortalTestApp
import org.radarbase.management.config.BasePostgresIntegrationTest
import org.radarbase.management.config.ContentTestUtil
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
import org.radarbase.management.repository.QueryParticipantRepository
import org.radarbase.management.repository.UserRepository
import org.radarbase.management.service.mapper.QueryContentGroupMapper
import org.radarbase.management.service.mapper.QueryContentMapper

import java.time.*



/**
 * Test class for the SubjectService class.
 *
 * @see SubjectService
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [ManagementPortalTestApp::class])
@Transactional
class ContentServiceTest(
    @Autowired private val queryEValuationService: QueryEValuationService,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val subjectRepository: SubjectRepository,
    @Autowired private val queryGroupRepository: QueryGroupRepository,
    @Autowired private val queryParticipantRepository: QueryParticipantRepository,
    @Autowired private val queryEvaluationRepository: QueryEvaluationRepository,
    @Autowired private val queryRepository: QueryRepository,
    @Autowired private val queryContentService: QueryContentService,
    @Autowired private val contentGroupRepository: QueryContentGroupRepository,
    @Autowired private val contentRepository: QueryContentRepository,
    @Autowired private val participantContentRepository: QueryParticipantContentRepository,
    @Autowired private val queryContentMapper: QueryContentMapper,
    @Autowired private val queryContentGroupMapper: QueryContentGroupMapper,
    @Autowired private val moduleRepository: ModuleRepository


) : BasePostgresIntegrationTest() {
    lateinit var userData: UserData
    lateinit var contentGroups: List<QueryContentGroup>
    lateinit var subject: Subject
    lateinit var queryGroup : QueryGroup
    lateinit var user: User


    private lateinit var queryContentServiceMock: QueryContentService

    private val contentNotificationRepositoryMock: ContentNotificationRepository = mock()
    private val notificationService: NotificationService = mock()

    private fun createNotification(sent: Boolean = false): ContentNotification {
        val user = User()
        user.setLogin("john.doe")
        val subject = Subject().apply { this.user = user }
        val group = QueryContentGroup().apply { contentGroupName = "Test Group"; id = 42L }

        return ContentNotification().apply {
            this.subject = subject
            this.contentGroup = group
            this.sent = sent
        }
    }
    @BeforeEach
    fun initTest() {
        queryContentServiceMock =
            spy(QueryContentService(
                contentRepository,
                queryGroupRepository,
                queryContentMapper,
                contentGroupRepository,
                queryParticipantRepository,
                subjectRepository,
                queryEvaluationRepository,
                participantContentRepository,
                queryContentGroupMapper,
                moduleRepository,

        notificationService,
        contentNotificationRepositoryMock
        ))


        user = userRepository.findAll()[0]
        subject = subjectRepository.findAll()[0]
        contentGroups = mutableListOf()
        userData = generateUserData(64.2,8, 50)

        queryGroup = queryGroupRepository.saveAndFlush(createQueryGroup());

        contentGroups += contentGroupRepository.saveAndFlush(ContentTestUtil.addContentGroup("GroupName", queryGroup))

        contentGroups += contentGroupRepository.saveAndFlush(ContentTestUtil.addContentGroup("GroupName 1", queryGroup))
        contentGroups += contentGroupRepository.saveAndFlush(ContentTestUtil.addContentGroup("GroupName 2", queryGroup))

        contentRepository.saveAndFlush(ContentTestUtil.addContentItem("value", "heading", ContentType.PARAGRAPH, queryGroup, contentGroups[0]))
        contentRepository.saveAndFlush(ContentTestUtil.addContentItem("value", "heading", ContentType.PARAGRAPH, queryGroup, contentGroups[1]))
        contentRepository.saveAndFlush(ContentTestUtil.addContentItem("value", "heading", ContentType.PARAGRAPH, queryGroup, contentGroups[0]))


        ContentTestUtil.addQueryParticipantContent(queryGroup, subject, contentGroups[0])
    }


    fun generateUserData(valueHeartRate: Double, valueSleep: Long, HRV: Long)  : UserData{
        val currentMonth = YearMonth.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM")

        val heartRateData =
            (1L until 8L).map { monthsAgo ->
                val month = currentMonth.minusMonths(monthsAgo).format(formatter)
                val value = valueHeartRate
                DataPoint(month, value.toDouble())
            }.reversed()


        val sleepData =   (1L until 8L).map { monthsAgo ->
            val month = currentMonth.minusMonths(monthsAgo).format(formatter)
            val value = valueSleep
            DataPoint(month, value.toDouble())
        }.reversed()


        val HRV =   (1L until 8L ).map { monthsAgo ->
            val month = currentMonth.minusMonths(monthsAgo).format(formatter)
            val value = valueSleep
            DataPoint(month, value.toDouble())
        }.reversed()

        return UserData(
            metrics = mapOf(
                "HEART_RATE" to heartRateData,
                "SLEEP_LENGTH" to sleepData,
                "HRV" to HRV)
        )
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



    @Test
    @Transactional
    fun testShouldSendNotification() {


        val lastSendNotification = ZonedDateTime.now()

        val trueEval = QueryEvaluation()
        trueEval.result = true
        var result = queryContentService.shouldSendNotification(listOf(trueEval), null);
        Assertions.assertEquals(true,result)


        val falseEval = QueryEvaluation()
        falseEval. result = false
        result = queryContentService.shouldSendNotification(listOf(falseEval), null)
       Assertions.assertEquals(false, result )


        result = queryContentService.shouldSendNotification(listOf(trueEval, falseEval), null)
        Assertions.assertEquals(true, result )

        result = queryContentService.shouldSendNotification(listOf(trueEval, falseEval, trueEval), null)
        Assertions.assertEquals(false, result )

        result = queryContentService.shouldSendNotification(listOf(trueEval, falseEval, falseEval, trueEval), null)
        Assertions.assertEquals(true, result )


        val trueEvalWithNotif = QueryEvaluation()
        trueEvalWithNotif.result = true
        trueEvalWithNotif.notificationScheduled = true

        val sixDaysAgo = ZonedDateTime.now().minusDays(6)

        result = queryContentService.shouldSendNotification(listOf(trueEval, trueEval, trueEval, trueEvalWithNotif), sixDaysAgo)
        Assertions.assertEquals(false, result)

        val sevenDaysAgo = ZonedDateTime.now().minusDays(7)

        result = queryContentService.shouldSendNotification(listOf(trueEval, falseEval, trueEval, trueEvalWithNotif), sevenDaysAgo)
        Assertions.assertEquals(true, result)

        result = queryContentService.shouldSendNotification(listOf(trueEval, trueEval, falseEval, falseEval, trueEval), null)
        Assertions.assertEquals(false, result )



    }

    @Test
    @Transactional
    fun testTryAssignNewContent() {
        for(contentGroup in contentGroups) {
            val sizeBefore =  participantContentRepository.findAll().size
            var result = queryContentService.tryAssignNewContent(queryGroup, subject)

            val sizeAfter =  participantContentRepository.findAll().size

            Assertions.assertEquals(sizeBefore + 1,sizeAfter)
        }


        val finalSize =  participantContentRepository.findAll().size
        var result = queryContentService.tryAssignNewContent(queryGroup, subject)
        val finalSizeAfter =  participantContentRepository.findAll().size

        Assertions.assertEquals(finalSize,finalSizeAfter)

        Assertions.assertEquals(3, finalSize)
        Assertions.assertEquals(result, null)
    }



    @Test
    @Transactional
    fun testTryAssignInactiveContentGroup() {

        val queryGroup1 = queryGroupRepository.saveAndFlush(createQueryGroup());
        val inactiveContentGroup = contentGroupRepository.saveAndFlush(ContentTestUtil.addContentGroup("GroupNameInactive", queryGroup1, ContentGroupStatus.INACTIVE))

        val sizeBefore =  participantContentRepository.findAll().size
        var result = queryContentService.tryAssignNewContent(queryGroup1, subject)
        val finalSizeAfter =  participantContentRepository.findAll().size


        Assertions.assertEquals(sizeBefore,finalSizeAfter)


    }



    @Test
    @Transactional
    fun testGetRandomAlreadyAssignedContent() {
        var result = queryContentService.getRandomAlreadyAssignedContent(queryGroup, subject)
        Assertions.assertEquals(result, null)

        val newContent = queryContentService.tryAssignNewContent(queryGroup, subject)
        result = queryContentService.getRandomAlreadyAssignedContent(queryGroup, subject)
        Assertions.assertEquals(result!!.contentGroupName, newContent!!.contentGroupName)
    }


    @Test
    @Transactional
    fun testProcessCompletedQueriesForParticipant() {
        val sizeBefore =  participantContentRepository.findAll().size

        var result = queryContentService.processCompletedQueriesForParticipant(subject.id!!)
        Assertions.assertEquals(result, false)

        var queryEvaluation = QueryEvaluation()
        queryEvaluation.queryGroup = queryGroup
        queryEvaluation.subject = subject
        queryEvaluation.createdDate = ZonedDateTime.now()
        queryEvaluation.result = true

        val queryParticipant = QueryParticipant()
        queryParticipant.queryGroup = queryGroup
        queryParticipant.subject = subject
        queryParticipant.assignedDate = ZonedDateTime.now()
        queryParticipant.createdBy = user

        queryParticipantRepository.saveAndFlush(queryParticipant)
        queryEvaluation = queryEvaluationRepository.saveAndFlush(queryEvaluation)

        result = queryContentService.processCompletedQueriesForParticipant(subject.id!!)



        val sizeAfter =   participantContentRepository.findAll().size

        Assertions.assertEquals(result, true)
        Assertions.assertEquals(sizeAfter , sizeBefore + 1)


        val queryEvaluationAfter = queryEvaluationRepository.findById(queryEvaluation.id).get()
        Assertions.assertEquals(true,queryEvaluationAfter.notificationScheduled)
    }

    @Test
    fun `sendScheduledNotifications does nothing if hour is not 16`() {
        // Spy the current time
        doReturn(LocalTime.of(10, 0)).whenever(queryContentServiceMock).getCurrentTime(ZoneId.of("Europe/London"))

        queryContentServiceMock.sendScheduledNotifications()

        verify(contentNotificationRepositoryMock, never()).findAllBySentIsFalse()
        verify(notificationService, never()).sendNotification(any(), any())
    }

    @Test
    fun `sendScheduledNotifications sends notifications at 16`() {
        val notification = createNotification()
        doReturn(LocalTime.of(16, 0)).whenever(queryContentServiceMock).getCurrentTime(ZoneId.of("Europe/London"))

        whenever(contentNotificationRepositoryMock.findAllBySentIsFalse())
            .thenReturn(listOf(notification))

        whenever(notificationService.sendNotification(any(), any())).thenReturn(true)

        queryContentServiceMock.sendScheduledNotifications()

        // Verify the notification is marked as sent
        Assertions.assertTrue(notification.sent)
        assert(notification.sentOn != null)

        verify(notificationService, times(1)).sendNotification(any(), any())
        verify(contentNotificationRepositoryMock, times(1)).saveAndFlush(notification)
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
