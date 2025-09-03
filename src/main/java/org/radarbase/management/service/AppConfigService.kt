package org.radarbase.management.service

import org.radarbase.management.domain.AppConfig
import org.radarbase.management.repository.AppConfigRepository
import org.radarbase.management.web.rest.AppConfigResource
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AppConfigService(private val repository: AppConfigRepository) {

    fun getMergedConfig(site: String?, userId: String?): Map<String?, AppConfig?> {
        val configMap = mutableMapOf<String?, AppConfig?>()


        repository.findBySiteIsNullAndUserIdIsNull()
            .forEach { configMap[it.key] = it }

        site?.let {
            repository.findBySiteAndUserIdIsNull(it)
                .forEach { configMap[it.key] = it }
        }

        userId?.let {
            repository.findByUserId(it)
                .forEach { configMap[it.key] = it }
        }

        log.info("returning config {}", configMap)

        return configMap
    }

    companion object {
        private val log = LoggerFactory.getLogger(AppConfigService::class.java)
    }
}
