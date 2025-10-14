package org.radarbase.management.repository


import org.radarbase.management.domain.PdfSummaryRequest
import org.radarbase.management.domain.Subject

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.RepositoryDefinition
import org.springframework.data.repository.history.RevisionRepository
import java.util.*

/**
 * Spring Data JPA repository for the Subject entity.
 */
@Suppress("unused")
@RepositoryDefinition(domainClass = PdfSummaryRequest::class, idClass = Long::class)
interface PdfSummaryRequestRepository : JpaRepository<PdfSummaryRequest, Long?>, RevisionRepository<PdfSummaryRequest, Long?, Int>,
    JpaSpecificationExecutor<PdfSummaryRequest> {


    fun findBySummaryCreatedOnIsNullAndEmailSentIsFalse(): List<PdfSummaryRequest>


    fun findBySubject(subject: Subject) : List<PdfSummaryRequest>


    fun findBySummaryIdAndSubject(summaryId: String, subject: Subject) : PdfSummaryRequest?

    @Query("""
        SELECT p
        FROM PdfSummaryRequest p
        WHERE p.requestedOn = (
            SELECT MAX(p2.requestedOn)
            FROM PdfSummaryRequest p2
            WHERE p2.subject = p.subject
        )
    """)
    fun findLatestPerSubject(): List<PdfSummaryRequest>


}
