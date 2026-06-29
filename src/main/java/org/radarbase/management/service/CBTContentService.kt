package org.radarbase.management.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.radarbase.management.service.catalog.CBTContent
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import com.fasterxml.jackson.core.type.TypeReference
import org.radarbase.management.domain.QueryContent
import org.radarbase.management.domain.QueryContentGroup
import org.radarbase.management.domain.QueryParticipantCbtAssignment
import org.radarbase.management.domain.QueryParticipantContent
import org.radarbase.management.domain.Subject
import org.radarbase.management.domain.enumeration.CbtRouteSelectionMode
import org.radarbase.management.repository.QueryParticipantCbtAssignmentRepository
import org.radarbase.management.web.rest.QueryResource
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime


enum class CBTContentType(val fileName: String) {
    CANNABIS("cannabis.json"),
    FEELING_CRITICISED("feeling_criticised.json"),
    SLEEP("sleep.json"),
    SUSPICIOUS_THOUGHTS("suspicious_thoughts.json"),
    VOICES("voices.json"),
    GETTING_OUT("getting_out.json")
}

@Service
class CBTContentService(private val objectMapper: ObjectMapper, val cbtAssignmentRepository: QueryParticipantCbtAssignmentRepository) {

    private var cbtContent: List<CBTContent> = emptyList()

    fun getContentFor(name: CBTContentType) :  List<CBTContent> {
        val resource = ClassPathResource("content/" + name.fileName)
        resource.inputStream.use {
            inputStream ->
            cbtContent = objectMapper.readValue(inputStream, object: TypeReference<List<CBTContent>>(){})

            return cbtContent
        }
    }

    fun getExercises(): List<CBTContent> = cbtContent


    fun createNewCBTAssignmentForParticipant(queryContent: QueryContent, participantContentGroup:  QueryParticipantContent, subject:Subject) {

        val cbtAssignment = QueryParticipantCbtAssignment();
        cbtAssignment.queryContent = queryContent
        cbtAssignment.queryParticipantContent = participantContentGroup
        cbtAssignment.createdDate = ZonedDateTime.now()
        cbtAssignment.subject = subject

        cbtAssignment.isArchived  = false
        cbtAssignment.cbtVersion = queryContent.cbtVersion
        cbtAssignment.cbtType = queryContent.cbtType


        if(queryContent.cbtRouteSelectionMode == CbtRouteSelectionMode.RANDOM) {
            val cbtContent =  getContentFor(CBTContentType.valueOf(queryContent.cbtType!!)).first()
            cbtAssignment.assignedCbtRoute = cbtContent.conditionalResponses.random().route

        } else {
            cbtAssignment.assignedCbtRoute = queryContent.cbtRoute
        }

        cbtAssignment.selectionMode = queryContent.cbtRouteSelectionMode

        cbtAssignmentRepository.save(cbtAssignment)

    }

    companion object {
        private val log = LoggerFactory.getLogger(CBTContentService::class.java)
    }
}
