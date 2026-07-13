package org.radarbase.management.repository

import org.radarbase.management.domain.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.RepositoryDefinition


@Suppress("unused")
@RepositoryDefinition(domainClass = QueryParticipantContent::class, idClass = Long::class)
interface QueryParticipantContentRepository : JpaRepository<QueryParticipantContent, Long> {

    fun findBySubjectAndQueryGroupAndIsArchivedFalse(subject: Subject, queryGroup: QueryGroup): List<QueryParticipantContent>

    fun findByQueryContentGroupIdAndIsArchivedFalse(queryContentGroupId: Long): List<QueryParticipantContent>

    fun findBySubjectAndIsArchivedFalse(subject: Subject) : List<QueryParticipantContent>

    fun findByQueryContentGroupAndSubjectAndIsArchivedFalse(queryContentGroup: QueryContentGroup, subject: Subject) : List<QueryParticipantContent>

    fun deleteAllByQueryContentGroupIdAndIsArchivedFalse(queryContentGroupId: Long)

    fun deleteByQueryGroupIdAndSubjectIdAndIsArchivedFalse(queryGroupId: Long, subjectId: Long)

}
