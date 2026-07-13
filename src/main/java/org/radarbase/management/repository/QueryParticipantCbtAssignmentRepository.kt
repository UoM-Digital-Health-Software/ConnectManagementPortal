package org.radarbase.management.repository


import org.radarbase.management.domain.QueryParticipantCbtAssignment
import org.springframework.data.jpa.repository.JpaRepository

@Suppress("unused")
interface QueryParticipantCbtAssignmentRepository :
    JpaRepository<QueryParticipantCbtAssignment, Long> {

    fun findAllBySubjectIdAndIsArchivedFalse(subjectId: Long): List<QueryParticipantCbtAssignment>

    fun findAllByQueryParticipantContentIdAndIsArchivedFalse(queryParticipantContentId: Long): List<QueryParticipantCbtAssignment>

    fun findOneByQueryParticipantContentIdAndQueryContentIdAndIsArchivedFalse(queryParticipantContentId: Long,  queryContentId: Long): QueryParticipantCbtAssignment



    fun findBySubjectIdAndQueryParticipantContentIdAndQueryContentIdAndIsArchivedFalse(
        subjectId: Long,
        queryParticipantContentId: Long,
        queryContentId: Long
    ): QueryParticipantCbtAssignment?

}
