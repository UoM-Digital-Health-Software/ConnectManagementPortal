package org.radarbase.management.web.rest
import org.radarbase.management.repository.SubjectRepository
import org.radarbase.management.service.AppConfigService
import org.radarbase.management.service.UserService
import org.radarbase.management.web.rest.errors.EntityName
import org.radarbase.management.web.rest.util.HeaderUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
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

        return service.isFeatureEnabled(subject?.activeProject?.projectName, user.id, user.login ?: "unknown" ,feature)
    }

    @PostMapping("/app-config/{feature}")
    fun getConfigWithCacheSize(@PathVariable feature: String?, @RequestBody(required = false) context: Map<String, Any>?): Boolean {
        val user = userService.getUserWithAuthorities() ?: throw IllegalArgumentException("User is not logged in")
        val subject = subjectRepository.findOneWithEagerBySubjectLogin(user.login)

        val safeContext = context ?: emptyMap()

        if(feature == null) throw IllegalArgumentException("Feature must be provided ")

        return service.isFeatureEnabled(subject?.activeProject?.projectName, user.id, user.login ?: "unknown" ,feature, safeContext)
    }



    @PostMapping("/cachesize")
    fun postCacheSize( @RequestBody(required = true) size: Int): ResponseEntity<Void> {
        val user = userService.getUserWithAuthorities() ?: throw IllegalArgumentException("User is not logged in")

        service.logCacheSize(user, size)

        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityDeletionAlert(EntityName.CACHESIZELOG, null)).build()

    }



    companion object {
        private val logger = LoggerFactory.getLogger(AppConfigResource::class.java)
    }
}
