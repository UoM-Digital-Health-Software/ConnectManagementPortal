package org.radarbase.management.repository

import org.radarbase.management.domain.ContentNotification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.RepositoryDefinition
import java.util.*

@Suppress("unused")
@RepositoryDefinition(domainClass = ContentNotification::class, idClass = Long::class)
interface ContentNotificationRepository: JpaRepository<ContentNotification, Long> {
    fun findAllBySentIsFalse() : List<ContentNotification>


}
