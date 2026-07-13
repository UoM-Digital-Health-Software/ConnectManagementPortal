package org.radarbase.management.service
import org.radarbase.management.domain.*
import org.radarbase.management.domain.enumeration.QueryLogicType
import org.radarbase.management.domain.enumeration.QueryReferenceType
import org.radarbase.management.domain.enumeration.QueryTimeFrame
import org.radarbase.management.repository.*
import org.radarbase.management.service.dto.QueryEvaluationDTO
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*
import org.springframework.scheduling.annotation.Scheduled
import java.time.ZoneId
import java.time.temporal.ChronoUnit


data class DataPoint(
    val month: String,
    val value: Double? = null,
    val histogramDataPoints: Map<String, Int> = mapOf()

)
data class UserData(
    val metrics: Map<String, List<DataPoint>>
)
@Service
@Transactional
public class QueryEValuationService(
    private val queryLogicRepository:  QueryLogicRepository,
    private val queryContentService: QueryContentService,
    private val queryEvaluationRepository: QueryEvaluationRepository,
    private val subjectRepository: SubjectRepository,
    private val queryParticipantRepository: QueryParticipantRepository,
    private val queryParticipantContentRepository: QueryParticipantContentRepository,
    private val awsService: AWSService,
    private val pdfSummaryRequestRepository: PdfSummaryRequestRepository,
) {
    fun evaluteQueryCondition(queryLogic: QueryLogic, userData: MutableMap<String, DataSummaryCategory>) : Boolean {
        return when(queryLogic.type) {
               QueryLogicType.CONDITION -> evaluateSingleCondition(queryLogic, userData)
               QueryLogicType.LOGIC -> evaluateLogicalCondition(queryLogic, userData)
            else -> false;
        }
    }

    private fun evaluateAgainstHistogramData(aggregatedData: MutableMap<String, Int>, expectedValue: String, timeFrameSize: Int) : Boolean {
        val sizeTarget = (timeFrameSize * QueryEvaluationOptions.minimumExpectedData).toInt()

        val numberOfAnswers = aggregatedData.values.sum();

        if(aggregatedData.isEmpty() || numberOfAnswers < sizeTarget) {
            return false
        }
        val maxRange = aggregatedData.maxByOrNull { it.value }

        val result = maxRange != null && maxRange.key == expectedValue
        return result
    }


    private fun evaluateAgainstAveragedData(relevantData: List<Double>, expectedValue: String, comparsionOperator: String, timeFrameSize: Int ): Boolean {

        val sizeTarget = (timeFrameSize * QueryEvaluationOptions.minimumExpectedData).toInt()

        if (relevantData.isEmpty() || relevantData.size < sizeTarget) {
            return false
        }
        val average = relevantData.average()

        return when (comparsionOperator){
            ">" -> average  > expectedValue.toDouble()
            "<" -> average < expectedValue.toDouble()
            ">=" -> average  >= expectedValue.toDouble()
            "<=" -> average <= expectedValue.toDouble()
            "=" -> average == expectedValue.toDouble()
            "!=" -> average != expectedValue.toDouble()
            else -> false
        }
    }

    fun aggregateDataForHistogramEvaluation(metric: String, currentTimeFrame: String, userData: MutableMap<String, DataSummaryCategory>, aggregatedData:  MutableMap<String, Int>)   {
        var questionnaireHistogramData :  MutableMap<String, Int>?  = mutableMapOf()

        when(metric.lowercase()) {
            "social" -> questionnaireHistogramData = userData[currentTimeFrame]?.questionnaire_histogram?.social
            "whereabouts"  -> questionnaireHistogramData = userData[currentTimeFrame]?.questionnaire_histogram?.whereabouts
            "sleep" -> questionnaireHistogramData = userData[currentTimeFrame]?.questionnaire_histogram?.sleep
        }

        if(questionnaireHistogramData != null) {
            for((range, count) in questionnaireHistogramData){
                aggregatedData[range] = aggregatedData.getOrDefault(range, 0) + count
            }
        }

    }

    private fun getRelevantDataForAveragedEvaluation(entity: String, metric:String, summary: DataSummaryCategory ) : Double? {
        val physicalData = summary.physical
        val questionnaireGroup = summary.questionnaire_slider

        val value = when (entity.lowercase()) {
            "physical" -> physicalData[metric.lowercase()]
            "questionnaire_group" -> questionnaireGroup[metric.lowercase()]
            else -> null
        }

        return value
    }

    fun getRollingAvgDates(query: Query): List<String>? {
        if(query.timeFrame != null && query.referenceType != null && query.referenceType == QueryReferenceType.ROLLING_AVG) {
            val targetDate = LocalDate.now()
            val endDate = getStartDateFromTimeFrameAndTargetDate(query.timeFrame!!, targetDate)


            val result = extractDatesToQuery(query.rollingWindow!!, endDate)

            return  result
        }
        return null
    }

    fun evaluateSingleCondition(
        queryLogic: QueryLogic,
        userData: MutableMap<String, DataSummaryCategory>
    ): Boolean {
        val query = queryLogic.query ?: return false

        val comparisonOperator = query.operator?.symbol ?: throw IllegalArgumentException("Comparsion operator is missing.")
        val entity = query.entity ?: throw IllegalArgumentException("Entity field is missing")
        val metric = query.field ?: throw IllegalArgumentException("Metric field is missing.")
        val expectedValue = query.value ?: throw IllegalArgumentException("Expected value is missing")
        val timeFrame = query.timeFrame ?: throw IllegalArgumentException("Timeframe is missing")
        val datesToQuery = extractDatesToQuery(timeFrame)


        val avgRollilngWindow =  mutableListOf<Double>()
        val rollingAvgDates = getRollingAvgDates(query)

        if(rollingAvgDates != null) {
            for(date in rollingAvgDates) {
                val summary = userData[date] ?: continue
                avgRollilngWindow += getRelevantDataForAveragedEvaluation(entity, metric, summary) ?: continue
            }

        }

        val avgEvalData = mutableListOf<Double>()
        val histogramEvalData = mutableMapOf<String, Int>()

        for (date in datesToQuery) {
            val summary = userData[date] ?: continue
            if (comparisonOperator == "IS") {
                aggregateDataForHistogramEvaluation(metric, date, userData, histogramEvalData)
            } else {
                avgEvalData += getRelevantDataForAveragedEvaluation(entity, metric, summary) ?: continue

            }
        }

        return if(query.referenceType == QueryReferenceType.ROLLING_AVG) {
            evaluateRollingAvgComparison(comparisonOperator, avgEvalData, avgRollilngWindow, expectedValue)
        } else {
            evaluateAbsoluteValue(comparisonOperator,histogramEvalData,expectedValue,datesToQuery,avgEvalData)
        }
    }

    fun evaluateRollingAvgComparison(   comparisonOperator: String,
                                  currentValues: List<Double>,
                                  referenceValues: List<Double>,
                                  expectedValue: String ): Boolean {

        if (currentValues.isEmpty() || referenceValues.isEmpty()) return false

        val currentAvg = currentValues.average()
        val referenceAvg = referenceValues.average()

        if (referenceAvg == 0.0) return false

        val percentChange = ((currentAvg - referenceAvg) / referenceAvg) * 100
        return when (comparisonOperator) {
            ">"  -> percentChange > expectedValue.toDouble()
            "<"  -> percentChange < expectedValue.toDouble()
            ">=" -> percentChange >= expectedValue.toDouble()
            "<=" -> percentChange <= expectedValue.toDouble()
            "="  -> kotlin.math.abs(percentChange - expectedValue.toDouble()) < 0.0001
            "!=" -> kotlin.math.abs(percentChange - expectedValue.toDouble()) >= 0.0001
            else -> false
        }
    }
    fun evaluateAbsoluteValue(comparisonOperator: String, histogramEvalData: MutableMap<String, Int>, expectedValue: String, datesToQuery: List<String>, avgEvalData: MutableList<Double>
    ): Boolean {
        return if (comparisonOperator == "IS") {
            evaluateAgainstHistogramData(histogramEvalData, expectedValue, datesToQuery.size)
        } else {
            evaluateAgainstAveragedData(avgEvalData, expectedValue, comparisonOperator, datesToQuery.size)
        }
    }
    fun evaluateLogicalCondition(queryLogic: QueryLogic, userData:  MutableMap<String, DataSummaryCategory>) : Boolean {
        val children = queryLogic.children ?: return false;

        val results = children.map {
            evaluteQueryCondition(it, userData)
        }

        return when (queryLogic.logicOperator.toString()) {
            "AND" -> results.all { it }
            "OR" -> results.any { it }
            else -> false;
        }
    }

    fun extractDatesToQuery(timeframe: QueryTimeFrame, originDate: LocalDate? = null): List<String> {
        val targetDate = originDate ?: LocalDate.now()
        val startDate = getStartDateFromTimeFrameAndTargetDate(timeframe, targetDate)
            ?: throw Exception("[extractDatesToQuery] startDate null")

        val dayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        val daysBetween = ChronoUnit.DAYS.between(startDate, targetDate)

        return (0 until daysBetween).map { startDate.plusDays(it).format(dayFormatter) }
    }


    //TODO: this will be replaced by a real data and automatic worker
    fun testLogicEvaluation(subject: Subject, project: String,  customUserData: UserData?) : MutableMap<String, Boolean>?  {
        val subjectLogin = subject.user?.login!!
        val subjectId = subject.id!!;

        val processedData = awsService.startProcessing(project, subjectLogin, QueryEvaluationOptions.source, QueryEvaluationOptions.aggregationLevel)?.data ?: throw IllegalArgumentException("There is no data for the user")

        val subjectOpt = subjectRepository.findById(subjectId)
        val queryParticipant = queryParticipantRepository.findBySubjectId(subjectId);
        val results: MutableMap<String, Boolean> = mutableMapOf()

        if(subjectOpt.isPresent && queryParticipant.isNotEmpty()) {

            val subject = subjectOpt.get();

            for (queryParticipant: QueryParticipant in queryParticipant) {

                val queryGroup = queryParticipant.queryGroup ?: continue
                val queryGroupId = queryGroup.id ?: continue

                val flatConditions = queryLogicRepository.findByQueryGroupId(queryGroupId)

                val root = buildLogicTree(flatConditions) ?: return results;

                val currentDate = LocalDate.now()
                val month = currentDate.month
                val monthName = month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)

                val result =  evaluteQueryCondition(root, processedData)

                saveQueryEvaluationResult(result, subject, queryGroup)

                results[queryGroup.name!!] = result;
            }
        }
        return results;
    }

    fun saveQueryEvaluationResult(result: Boolean, subject: Subject, queryGroup: QueryGroup) {
        val newQueryEvaluation = QueryEvaluation();

        newQueryEvaluation.queryGroup = queryGroup
        newQueryEvaluation.subject = subject
        newQueryEvaluation.createdDate        = ZonedDateTime.now()
        newQueryEvaluation.result = result
        newQueryEvaluation.notificationScheduled = false

        queryEvaluationRepository.save(newQueryEvaluation);

        queryEvaluationRepository.flush();
    }
    fun buildLogicTree(conditions: List<QueryLogic>): QueryLogic? {
        val map = conditions.associateBy { it.id  }.toMutableMap()

        var root : QueryLogic? = null

        for(condition in conditions) {
            if (condition.parent?.id != null) {
                val parent = map[condition.parent?.id]
                parent?.children?.add(condition)
            }
             else {
                    root = condition;
                }
            }


        return root;
    }

    fun getEvaluationForParticipant(participant: Subject):  List<QueryEvaluationDTO> {
        val allEvaluations = queryEvaluationRepository.findBySubject(participant)

        var result = allEvaluations.map { queryEvaluation ->
            QueryEvaluationDTO(queryGroupName = queryEvaluation.queryGroup?.name, evaluationDate = queryEvaluation.createdDate, result = queryEvaluation.result, notificationScheduled = queryEvaluation.notificationScheduled)
        }
        result = result.sortedByDescending { it.evaluationDate }
        return result
    }



    @Scheduled(cron = "0 0 5 * * ?")
    fun evaluateQueries() {
        log.info("[evaluateQueries] before time ")
        val now = TimeUtils.getCurrentTime(ZoneId.of("Europe/London"))
        if (now.hour != 5) {
            return
        }
        log.info("[evaluateQueries] running")
        val queryParticipantList =  queryParticipantRepository.findAll()
        log.info("[evaluateQueries] queryParticipantList {}", queryParticipantList.size)
        for(queryParticipant  in queryParticipantList) {

            val participant = queryParticipant.subject
            val queryGroup  = queryParticipant.queryGroup
            log.info("[evaluateQueries] participant {}", participant)
            log.info("[evaluateQueries] queryGroup {}", queryGroup)


            if(participant == null  || queryGroup == null ) {
                continue
            }
            val latestPDFSummary = pdfSummaryRequestRepository.findFirstBySubjectOrderByRequestedOnDesc(participant!!)


            if(latestPDFSummary?.emailSent == true)  {
                val project = participant.activeProject!!.projectName!!
                testLogicEvaluation(participant, project, null);

                queryContentService.processCompletedQueriesForParticipant(participant.id!!)
            }
        }
    }

    fun getStartDateFromTimeFrameAndTargetDate(timeFrameName: QueryTimeFrame, targetDate: LocalDate) : LocalDate?  {
        val result = when (timeFrameName.name) {
            "PAST_WEEK" -> targetDate.minusDays(7)
            "PAST_MONTH" -> targetDate.minusDays(30)
            "PAST_6_MONTH" -> targetDate.minusDays(180)
            "PAST_YEAR" -> targetDate.minusDays(365)
            else -> throw Exception("No timeframe provided")
        }

        return result
    }


    @Transactional
    fun  removeQueryParticipantContent(queryGroupId: Long, subjectId: Long) {
        queryParticipantContentRepository.deleteByQueryGroupIdAndSubjectIdAndIsArchivedFalse(queryGroupId, subjectId);
    }

    companion object {
        private val log = LoggerFactory.getLogger(QueryBuilderService::class.java)
    }
}
