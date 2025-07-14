package org.radarbase.management.service.dto

import org.radarbase.management.domain.enumeration.ContentGroupStatus
import org.radarbase.management.service.dto.QueryContentDTO

class QueryContentGroupDTO {
    var contentGroupName: String? = null
    var queryGroupId: Long? = null
    var queryContentDTOList: List<QueryContentDTO>? = null
    var id: Long? = null
    var status: ContentGroupStatus? = null
}
