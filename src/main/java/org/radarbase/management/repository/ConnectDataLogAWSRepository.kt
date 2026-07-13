package org.radarbase.management.repository

import org.radarbase.management.domain.ConnectDataLogAWS
import org.radarbase.management.domain.support.ConntetDataLogView
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.RepositoryDefinition
import org.springframework.data.repository.history.RevisionRepository
import org.springframework.data.repository.query.Param


@Suppress("unused")
@RepositoryDefinition(domainClass = ConnectDataLogAWS::class, idClass = Long::class)
interface ConnectDataLogAWSRepository  : JpaRepository<ConnectDataLogAWS, Long?>,
    RevisionRepository<ConnectDataLogAWS, Long?, Int>,
    JpaSpecificationExecutor<ConnectDataLogAWS> {


    @Query(
        value = "SELECT DISTINCT ON (u.\"user_id\", u.\"data_grouping_type\") * " +
                "FROM connect_data_log_aws u " +
                "WHERE u.\"user_id\" IN (:userIds) " +
                "AND u.\"data_grouping_type\" IS NOT NULL " +
                "ORDER BY u.\"user_id\", u.\"data_grouping_type\", u.time DESC", nativeQuery = true
    )
    fun findLatestLogsByUserIds(@Param("userIds") userIds: List<String?>?): List<ConnectDataLogAWS?>?
}
