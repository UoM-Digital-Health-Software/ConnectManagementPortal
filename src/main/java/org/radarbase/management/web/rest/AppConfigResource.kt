package org.radarbase.management.web.rest

import org.radarbase.management.domain.AppConfig
import org.radarbase.management.service.AppConfigService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/config")
class AppConfigResource(private val service: AppConfigService) {

    @GetMapping("/{site}/{userId}")
    fun getConfig(@PathVariable site: String, @PathVariable userId: String): Map<String?, AppConfig?> {
        return service.getMergedConfig(site, userId)
    }

    @GetMapping("/{site}")
    fun getSiteConfig(@PathVariable site: String): Map<String?, AppConfig?> {
        return service.getMergedConfig(site, null)
    }
}
