package org.radarbase.management.repository

import org.radarbase.management.domain.CacheSizeLog
import org.radarbase.management.domain.PdfSummaryRequest
import org.radarbase.management.domain.Subject
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.RepositoryDefinition
import org.springframework.data.repository.history.RevisionRepository
import java.time.ZonedDateTime


@Suppress("unused")
@RepositoryDefinition(domainClass = CacheSizeLog::class, idClass = Long::class)
interface CacheSizeLogRepository : JpaRepository<CacheSizeLog, Long>  {


    fun existsByUserIdAndCreatedOn(userId: Long, createdOn: ZonedDateTime): Boolean



}





