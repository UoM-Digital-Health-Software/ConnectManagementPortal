package org.radarbase.management.config

import org.radarbase.management.service.SaltProvider
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class FixedSaltTestConfig {
    @Bean
    fun saltProvider(): SaltProvider = object : SaltProvider {
        override fun getSalt() = "10"   // deterministic for tests
    }
}
