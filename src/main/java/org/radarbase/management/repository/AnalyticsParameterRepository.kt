package org.radarbase.management.repository

import org.radarbase.management.domain.Analytics
import org.radarbase.management.domain.AnalyticsParameter
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.RepositoryDefinition


@Suppress("unused")
@RepositoryDefinition(domainClass = AnalyticsParameter::class, idClass = Long::class)
interface AnalyticsParameterRepository  : JpaRepository<AnalyticsParameter, Long>  {

}
