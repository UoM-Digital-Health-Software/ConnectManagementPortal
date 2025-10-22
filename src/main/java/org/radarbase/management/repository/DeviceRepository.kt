package org.radarbase.management.repository

import org.radarbase.management.domain.Device
import org.radarbase.management.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.RepositoryDefinition
import java.util.*


@Suppress("unused")
@RepositoryDefinition(domainClass = Device::class, idClass = Long::class)
interface DeviceRepository : JpaRepository<Device, Long> {

    fun findByUser(user: User?): List<Device>
    fun findByUserAndToken(user: User?, token: String?): Optional<Device>
}


