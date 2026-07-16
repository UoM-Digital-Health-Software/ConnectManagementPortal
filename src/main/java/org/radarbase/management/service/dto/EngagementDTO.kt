package org.radarbase.management.service.dto

import java.time.Instant

class EngagementDTO {
    var firstViewTime: Instant? = null
    var lastViewTime: Instant? = null

    var durationSeconds: Long? = null
    var analytics: MutableList<AnalyticsDTO> = mutableListOf()
}
