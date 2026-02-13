package org.radarbase.management.service.dto

import java.time.ZonedDateTime

data class QueryEvaluationDTO (
    var queryGroupName: String?,
    var evaluationDate: ZonedDateTime?,
    var result : Boolean?,
    var notificationScheduled: Boolean?)






