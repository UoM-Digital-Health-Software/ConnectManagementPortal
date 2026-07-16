package org.radarbase.management.repository

import org.radarbase.management.domain.Analytics
import org.radarbase.management.domain.QueryParticipant
import org.radarbase.management.domain.Subject
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.RepositoryDefinition
import java.time.ZonedDateTime


@Suppress("unused")
@RepositoryDefinition(domainClass = Analytics::class, idClass = Long::class)
interface AnalyticsRepository  : JpaRepository<Analytics, Long>  {

    fun findAllByParticipantOrderByDateAsc(participant: Subject): List<Analytics>
}
