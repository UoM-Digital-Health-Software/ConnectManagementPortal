package org.radarbase.management.service.dto

import org.radarbase.management.domain.enumeration.ContentType
import org.radarbase.management.service.CBTContentType

class QueryContentDTO {
    var id : Long? = null ;
    var type: ContentType? = null
    var heading: String? = null
    var value: String?  = null
    var imageBlob: String? = null
    var imageAltText: String? = null
    var queryGroupId: Long? = null
    var queryContentGroupId: Long? = null
    var resourceId : Long? = null

    var cbtVersion : String? = null
    var cbtType : CBTContentType? = null
    var cbtRoute   : String? = null
}
