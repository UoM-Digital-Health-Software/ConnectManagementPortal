package org.radarbase.management.repository

import org.radarbase.management.domain.AppConfig
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.RepositoryDefinition

@Suppress("unused")
@RepositoryDefinition(domainClass = AppConfig::class, idClass = Long::class)
interface AppConfigRepository  : JpaRepository<AppConfig, Long>  {
    fun findBySiteIsNullAndUserIdIsNull(): List<AppConfig>
    fun findBySiteAndUserIdIsNull(site: String): List<AppConfig>
    fun findByUserId(userId: Long): List<AppConfig>
}
