package org.radarbase.management.repository


import org.radarbase.management.domain.QueryParticipantCbtAssignment
import org.springframework.data.jpa.repository.JpaRepository

@Suppress("unused")
interface QueryParticipantCbtAssignmentRepository :
    JpaRepository<QueryParticipantCbtAssignment, Long> {

    fun findAllBySubjectId(subjectId: Long): List<QueryParticipantCbtAssignment>

    fun findAllByQueryParticipantContentId(queryParticipantContentId: Long): List<QueryParticipantCbtAssignment>

    fun findBySubjectIdAndQueryParticipantContentIdAndQueryContentId(
        subjectId: Long,
        queryParticipantContentId: Long,
        queryContentId: Long
    ): QueryParticipantCbtAssignment?

}
