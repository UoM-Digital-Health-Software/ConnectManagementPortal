package org.radarbase.management.service.dto

import org.radarbase.management.domain.enumeration.AuditEvent
import java.time.Instant
import java.time.LocalDate

class AnalyticsDTO {
    var date: Instant? = null
    var participantId: Long? = null
    var externalId : String? = null
    var action: AuditEvent? = null
    var additionalData: String? = null
    var pageUrl: String? = null
    var category: String? = null

    var duration: Long? = null
}
