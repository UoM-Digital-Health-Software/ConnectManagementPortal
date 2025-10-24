package org.radarbase.management.repository;


import org.radarbase.management.domain.ConnectDataLog;
import org.radarbase.management.domain.support.ConntetDataLogView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.RepositoryDefinition;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@RepositoryDefinition(domainClass = ConnectDataLog.class, idClass = String.class)
public interface ConnectDataLogRepository extends JpaRepository<ConnectDataLog, String>,
        RevisionRepository<ConnectDataLog, String, Integer> {

    @Query(
            value = "SELECT * FROM connect_data_log u WHERE  \"userId\" is not null and \"userId\"=?1  and \"dataGroupingType\" is not null and \"dataGroupingType\"=?2 ORDER BY u.time DESC",
            nativeQuery = true)
    Optional<ConnectDataLog> findDataLogsByUserIdAndDataGroupingType( String userId, String dataGroupingType);



    @Query(
            value = "SELECT DISTINCT ON (u.\"userId\", u.\"dataGroupingType\") * " +
                    "FROM connect_data_log u " +
                    "WHERE u.\"userId\" IN (:userIds) " +
                    "AND u.\"dataGroupingType\" IS NOT NULL " +
                    "ORDER BY u.\"userId\", u.\"dataGroupingType\", u.time DESC",
            nativeQuery = true
    )
    List<ConntetDataLogView> findLatestLogsByUserIds(@Param("userIds") List<String> userIds);

}
