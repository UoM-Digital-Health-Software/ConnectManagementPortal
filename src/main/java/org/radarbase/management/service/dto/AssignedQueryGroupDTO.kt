package org.radarbase.management.service.dto

import org.radarbase.management.domain.User
import java.time.ZonedDateTime

class AssignedQueryGroupDTO {
    lateinit var name : String
    var id: Long? = null
    var createdBy : User? = null
    var assignedDate : ZonedDateTime? = null
}
