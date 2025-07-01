package org.radarbase.management.repository

import org.radarbase.management.domain.Module
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.RepositoryDefinition
import org.springframework.data.repository.history.RevisionRepository


@RepositoryDefinition(domainClass = Module::class, idClass = Long::class)
    interface ModuleRepository  : JpaRepository<Module, Long>, RevisionRepository<Module, Long, Int>  {
}
