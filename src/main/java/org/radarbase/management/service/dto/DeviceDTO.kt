package org.radarbase.management.service.dto

import org.radarbase.management.domain.enumeration.PlatformType

class DeviceDTO {
    var platform: PlatformType? = null
    var token: String? = null


    override fun toString(): String {
        return "DeviceDTO [" +
                "platform=" + platform +
                ", token=" + token +
                "]"
    }
}
