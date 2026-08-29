package org.radarbase.management.service.dto

data class LatestLogDTO(
    val projectId: String?,
    val userId: String?,
    val dataGroupingType: String?,
    val latestTime: java.time.Instant?
)
