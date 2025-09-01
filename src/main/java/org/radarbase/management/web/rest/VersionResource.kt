package org.radarbase.management.web.rest

import org.radarbase.management.config.ManagementPortalProperties
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/versions")
class VersionRescource(
    private val managementPortalProperties: ManagementPortalProperties
) {
    @GetMapping("min-required-version")
    fun getMinRequiredVersion(): Map<String, String> {
        val minVersion = managementPortalProperties.getMinRequiredVersion()
        return mapOf(
            "ios" to minVersion.getIos(),
            "android" to minVersion.getAndroid()
        )
    }
}
