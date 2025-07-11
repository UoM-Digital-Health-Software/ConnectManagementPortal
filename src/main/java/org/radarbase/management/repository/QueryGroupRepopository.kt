package org.radarbase.management.repository

import org.radarbase.management.domain.QueryGroup
import org.radarbase.management.domain.QueryLogic
import org.radarbase.management.domain.SourceData
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.RepositoryDefinition
import org.springframework.data.repository.query.Param

@Suppress("unused")
@RepositoryDefinition(domainClass = QueryGroup::class, idClass = Long::class)
interface QueryGroupRepository : JpaRepository<QueryGroup, Long> {

    @Query(
        """
        SELECT CASE WHEN COUNT(q) > 0 THEN true ELSE false END
        FROM QueryGroup q
        WHERE q.name = :name AND (:excludeId IS NULL OR q.id <> :excludeId)
    """
    )
    fun existsByNameAndIdNot(
        @Param("name") name: String,
        @Param("excludeId") excludeId: Long?
    ): Boolean
}
