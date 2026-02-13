package org.radarbase.management

import org.radarbase.management.domain.Query
import org.radarbase.management.domain.QueryGroup
import org.radarbase.management.domain.QueryLogic
import org.radarbase.management.domain.enumeration.*
import org.radarbase.management.repository.QueryGroupRepository
import org.radarbase.management.repository.QueryLogicRepository
import org.radarbase.management.repository.QueryRepository
import org.radarbase.management.repository.UserRepository
import java.time.ZonedDateTime

object QueryUtil {

    fun createQueryGroup(userRepository: UserRepository, queryGroupRepository: QueryGroupRepository): QueryGroup {
        val user = userRepository.findAll()[0];

        val queryGroup = QueryGroup();
        queryGroup.name = "TestQueryGroup"
        queryGroup.description = "description"
        queryGroup.createdBy = user;
        queryGroup.createdDate = ZonedDateTime.now();

        return queryGroupRepository.saveAndFlush(queryGroup) ;
    }

    fun createQuery(queryGroup: QueryGroup, physicalMetric: PhysicalMetric, queryOperator: ComparisonOperator, timeframe: QueryTimeFrame, value: String, queryRepository: QueryRepository)  : Query {
        var query = Query();

        query.queryGroup = queryGroup
        query.field = physicalMetric.toString()
        query.operator = queryOperator
        query.value = value
        query.timeFrame = timeframe
        query.entity = "physical"

        return queryRepository.saveAndFlush(query);
    }

    fun createQueryLogic(queryGroup: QueryGroup, type: QueryLogicType, logicOperator: QueryLogicOperator?, query:Query?, parentQueryLogic : QueryLogic?, queryLogicRepository: QueryLogicRepository ) : QueryLogic {
        val queryLogic = QueryLogic() ;
        queryLogic.queryGroup = queryGroup
        queryLogic.type = type
        queryLogic.logicOperator = logicOperator
        queryLogic.query = query;
        queryLogic.parent = parentQueryLogic

        return queryLogicRepository.saveAndFlush(queryLogic)
    }

}
