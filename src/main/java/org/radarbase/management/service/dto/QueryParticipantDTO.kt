package org.radarbase.management.service.dto

import org.radarbase.management.domain.User
import java.time.ZonedDateTime

class QueryParticipantDTO {

    var queryGroupId: Long? = null
    var subjectId: Long? = null
    var createdBy: User? = null
    var queryGroupName: String? = null
    var assignedDate: ZonedDateTime? = null

    override fun toString(): String {
        return ("QueryParticipantDTO{" +
                "queryGroupId='" +
                queryGroupId +
                '\'' +
                "queryGroupName='" +
                queryGroupName +
                '\'' +
                ", subjectId='" +
                subjectId +
                '\'' +
                ", createdBy='" +
                createdBy +
                '\'' +
                '\'' +
                ", assignedDate='" +
                assignedDate +
                '\'' +
                "}")
    }
}
