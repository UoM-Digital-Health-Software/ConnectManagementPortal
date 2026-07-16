package org.radarbase.management.service

import org.radarbase.management.domain.Analytics
import org.radarbase.management.domain.AnalyticsParameter
import org.radarbase.management.domain.Subject
import org.radarbase.management.domain.enumeration.AuditEvent
import org.radarbase.management.repository.AnalyticsParameterRepository
import org.radarbase.management.repository.AnalyticsRepository
import org.radarbase.management.repository.SubjectRepository
import org.radarbase.management.service.dto.AnalyticsDTO
import org.radarbase.management.service.dto.EngagementDTO
import org.radarbase.management.service.dto.PageDTO
import org.radarbase.management.web.rest.AnalyticsResource
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant


@Service
@Transactional
class AnalyticsService(
    private val analyticsRepository: AnalyticsRepository,
    private val subjectRepository: SubjectRepository,
    private val analyticsParameterRepository: AnalyticsParameterRepository
    ) {


    fun create(pageDTO: PageDTO, participantId: Long) {

        val subjectOpt = subjectRepository.findById(participantId);
        if(subjectOpt.isPresent) {
            val subject = subjectOpt.get()

            var analytics : Analytics = Analytics()
            analytics.participant = subject
            analytics.action = AuditEvent.VIEW
            analytics.pageUrl = pageDTO.url
            analytics.category = pageDTO.title
            analytics.date = Instant.now()
            analytics = analyticsRepository.save(analytics)

            if(pageDTO.parameters.isNotEmpty()) {
                for(parameter in pageDTO.parameters) {
                    saveAnalyticsParameter(analytics, parameter.parameterName, parameter.parameterValue)

                }

            }
        }
    }


    fun createAppChange(auditEvent: AuditEvent, participantId: Long) {
        val subjectOpt = subjectRepository.findById(participantId);
        if(subjectOpt.isPresent && (auditEvent == AuditEvent.APP_BACKGROUND || auditEvent == AuditEvent.APP_FOREGROUND)) {

            val analytics : Analytics = Analytics()
            analytics.participant = subjectOpt.get()
            analytics.action = auditEvent
            analyticsRepository.saveAndFlush(analytics)
        }
    }


    fun saveAnalyticsParameter(analytics: Analytics, name: String, value: String) {

        val analyticsParameter = AnalyticsParameter()
        analyticsParameter.analytics = analytics
        analyticsParameter.parameterName = name
        analyticsParameter.parameterValue = value
        analyticsParameterRepository.save(analyticsParameter)
    }


    fun getEngagementForParticipant(subject: Subject) : MutableList<EngagementDTO> {
        val analytics = analyticsRepository.findAllByParticipantOrderByDateAsc(subject)
        val engagements : MutableList<EngagementDTO>  = mutableListOf()


        var engagement : EngagementDTO? = null
        val iterator = analytics.listIterator()


        while(iterator.hasNext()) {
            val current =  iterator.next()

            if(isLifecycleEvent(current)) {
                continue
            }

            if(engagement == null) {
                engagement = startEngagement(current)
            } else {
                addAnalyticsToEngagement(current, engagement)
            }


            if(isCurrentAnalyticsLast(iterator, current)) {
                endEngagement(iterator,engagement, current)
                engagements.add(engagement)
                engagement = null
            }
        }

        logger.info("before return {}", engagements.get(0))
        return engagements


    }

    fun startEngagement(current: Analytics) : EngagementDTO {
        val engagementDTO = EngagementDTO()
        engagementDTO.firstViewTime = current.date

        addAnalyticsToEngagement(current, engagementDTO)

        return engagementDTO
    }

    fun addAnalyticsToEngagement(analytics: Analytics, engagementDTO: EngagementDTO) {
        val analyticsDTO = AnalyticsDTO()
        analyticsDTO.action = analytics.action
        analyticsDTO.date = analytics.date
        analyticsDTO.pageUrl = analytics.pageUrl
        analyticsDTO.category = analytics.category

        engagementDTO.analytics.add(analyticsDTO)
    }

    fun isCurrentAnalyticsLast(iterator:  ListIterator<Analytics>, analyticsCurrent: Analytics): Boolean {

        if(iterator.hasNext()) {
            val analyticsNext = iterator.next()
            val isLast = nextAnalyticsIsNewEngagement(analyticsCurrent, analyticsNext)
            iterator.previous()
            return isLast

        }

        return true
    }


    fun nextAnalyticsIsNewEngagement(current: Analytics, next: Analytics) : Boolean{
        if (isLifecycleEvent(current) || isLifecycleEvent(next)) {
            return true
        }
        val gap = Duration.between(current.date!!, next.date!!)

        return gap.toMinutes() > 30;
    }

    fun isLifecycleEvent(analytics: Analytics): Boolean {
        return analytics.action == AuditEvent.APP_BACKGROUND ||
                analytics.action == AuditEvent.APP_FOREGROUND
    }

    fun endEngagement(iterator:  ListIterator<Analytics>, engagement: EngagementDTO, current: Analytics) {
        engagement.lastViewTime = current.date
        if(iterator.hasNext()) {
            val analyticsNext = iterator.next()
            if(analyticsNext.action == AuditEvent.APP_BACKGROUND) {

                engagement.lastViewTime = analyticsNext.date
                addAnalyticsToEngagement(analyticsNext, engagement)
            }

            iterator.previous()
        }


        val durationSeconds = calculateEngagementDurationSeconds(engagement)
        engagement.durationSeconds = durationSeconds

    }

    fun calculateEngagementDurationSeconds(engagement: EngagementDTO) : Long {
        if(engagement.firstViewTime != null && engagement.lastViewTime != null) {
            val duration = Duration.between(engagement.firstViewTime, engagement.lastViewTime)
            val durationSeconds = duration.getSeconds()
            val pageViewCount: Int = engagement.analytics.size
            val averageSeconds = durationSeconds / pageViewCount
            val finalPageViewSeconds = averageSeconds
            val totalDuration = durationSeconds + finalPageViewSeconds


            val first = engagement.analytics.first()
            val last = engagement.analytics.last()

            val iterator = engagement.analytics.listIterator()

            while(iterator.hasNext()) {
                val current = iterator.next()

                if(iterator.hasNext())
                {
                    val next = iterator.next()
                    iterator.previous()
                    val duration = Duration.between(current.date, next.date)
                    current.duration = duration.seconds

                } else if(current.action != AuditEvent.APP_BACKGROUND) {
                    current.duration = averageSeconds
                }
            }

            return totalDuration
        }

        return 0

    }



    companion object {
        private val logger = LoggerFactory.getLogger(AnalyticsResource::class.java)
    }
}


