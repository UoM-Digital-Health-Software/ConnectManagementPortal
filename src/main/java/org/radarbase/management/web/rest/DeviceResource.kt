package org.radarbase.management.web.rest

import org.radarbase.management.service.NotificationService
import org.radarbase.management.service.UserService
import org.radarbase.management.service.dto.DeviceDTO
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.lang.IllegalArgumentException
import java.net.URISyntaxException
import java.security.Principal
import javax.validation.Valid


@RestController
@RequestMapping("/api")
class DeviceResource(@Autowired private val notificationService: NotificationService , @Autowired private val userService: UserService) {



    @PostMapping("/device")
    @Throws(URISyntaxException::class)
    fun saveDevice(
        @RequestBody deviceDTO: @Valid DeviceDTO?,

    ) {
        val currentUser = userService.getUserWithAuthorities()  ?: throw IllegalArgumentException("No user logged in")

        deviceDTO?.let {
            notificationService.registerDevice(deviceDTO, currentUser.login)
        } ?: throw IllegalArgumentException("Device must be provided ")
    }


    companion object {
        private val log = LoggerFactory.getLogger(DeviceResource::class.java)

    }
}
