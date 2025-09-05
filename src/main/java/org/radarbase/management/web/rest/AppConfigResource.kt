package org.radarbase.management.web.rest

import org.radarbase.management.domain.AppConfig
import org.radarbase.management.repository.SubjectRepository
import org.radarbase.management.service.AppConfigService
import org.radarbase.management.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class AppConfigResource(@Autowired  private val service: AppConfigService,
                        @Autowired  private val userService: UserService,
                        @Autowired private val subjectRepository: SubjectRepository) {

    @GetMapping("/app-config/{feature}")
    fun getConfig( @PathVariable feature: String?): Boolean {
        val user = userService.getUserWithAuthorities() ?: throw IllegalArgumentException("User is not logged in")
        val subject = subjectRepository.findOneWithEagerBySubjectLogin(user.login)

        if(feature == null) throw IllegalArgumentException("Feature must be provided ")

        return service.isFeatureEnabled(subject?.activeProject?.projectName, user.login ?: "unknown" ,feature)
    }



    companion object {
        private val logger = LoggerFactory.getLogger(AppConfigResource::class.java)
    }
}
