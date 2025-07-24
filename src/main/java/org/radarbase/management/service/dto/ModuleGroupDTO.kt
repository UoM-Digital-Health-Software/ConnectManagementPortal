package org.radarbase.management.service.dto
import org.radarbase.management.domain.Module

data class ModuleGroupDTO(
    val groupName: String,
    val modules: List<Module>
)
