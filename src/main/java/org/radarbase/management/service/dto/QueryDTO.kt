package org.radarbase.management.service.dto

import org.radarbase.management.domain.Query
import org.radarbase.management.domain.enumeration.ComparisonOperator
import org.radarbase.management.domain.enumeration.QueryBuilderEntities
import org.radarbase.management.domain.enumeration.QueryReferenceType
import org.radarbase.management.domain.enumeration.QueryTimeFrame
import org.radarbase.management.web.rest.QueryResource
import org.slf4j.LoggerFactory
import javax.persistence.Column

class QueryDTO {
    constructor(query: Query?) {
        this.field = query?.field
        this.operator = query?.operator
        this.value = query?.value
        this.timeFrame = query?.timeFrame

       this.entity = query?.entity?.let { enumValueOf<QueryBuilderEntities>(it) } ?: throw IllegalArgumentException("Query Entity is null or invalid")
        this.referenceType = query?.referenceType?.symbol
        this.rollingWindow = query?.rollingWindow

    }

    constructor(metric: String?, operator: ComparisonOperator?, value: String?, timeFrame: QueryTimeFrame?, entity: String? , referenceType: String? = "absolute" , rollingWindow: QueryTimeFrame? = null ) {

       this.field = metric.toString();
        this.operator = operator
        this.value = value
        this.timeFrame = timeFrame
        this.entity = entity?.let { enumValueOf<QueryBuilderEntities>(it) } ?: throw IllegalArgumentException("Query Entity is null or invalid")
        this.referenceType = referenceType
        this.rollingWindow = rollingWindow

    }



    var entity: QueryBuilderEntities? = null
    var field: String? = null
    var operator: ComparisonOperator? = null
    var value: String? = null
    var timeFrame : QueryTimeFrame? = null

    var referenceType: String? = null
    var rollingWindow: QueryTimeFrame? = null

    override fun toString(): String {
        return ("QueryDTO{"
                + "metric='" + field + '\''
                + ", operator='" + operator + '\''
                + ", value='" + value + '\''
                + ", time_frame='" + timeFrame + '\''
                + ", referenceType='" + referenceType + '\''
                + ", rollingWindow='" + rollingWindow + '\''
                + "}")
    }

    companion object {
        private val log = LoggerFactory.getLogger(QueryDTO::class.java)
    }
}
