package org.radarbase.management.repository

import org.radarbase.management.domain.LatestMeasurementDatesTracker
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.RepositoryDefinition
import org.springframework.data.repository.history.RevisionRepository


@Suppress("unused")
@RepositoryDefinition(domainClass = LatestMeasurementDatesTracker::class, idClass = Long::class)
interface LatestMeasurementDatesTrackerRepository : JpaRepository<LatestMeasurementDatesTracker, Long?>,
    RevisionRepository<LatestMeasurementDatesTracker, Long?, Int>,
    JpaSpecificationExecutor<LatestMeasurementDatesTracker>
{
    fun findFirstByGeneratedFalseOrderByRequestedOnDesc(): LatestMeasurementDatesTracker?

    fun findFirstByGeneratedTrueOrderByRequestedOnDesc(): LatestMeasurementDatesTracker?


    fun findFirstByGeneratedTrueAndLoadedFalseOrderByRequestedOnDesc(): LatestMeasurementDatesTracker?

    fun findFirstByGeneratedTrueAndLoadedTrueOrderByRequestedOnDesc(): LatestMeasurementDatesTracker?


    fun findBySummaryId(summaryId: String): List<LatestMeasurementDatesTracker>

}
