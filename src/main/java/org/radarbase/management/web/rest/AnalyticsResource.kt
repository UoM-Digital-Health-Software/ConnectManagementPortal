package org.radarbase.management.web.rest

import org.radarbase.management.domain.enumeration.AuditEvent
import org.radarbase.management.repository.SubjectRepository
import org.radarbase.management.service.AnalyticsService
import org.radarbase.management.service.SubjectService
import org.radarbase.management.service.UserService
import org.radarbase.management.service.dto.PageDTO
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.radarbase.management.security.Constants

@RestController
@RequestMapping("/api")
class AnalyticsResource(@Autowired  private val analyticsService: AnalyticsService,
                        @Autowired private val userService: UserService,
    @Autowired private val subjectService: SubjectService,
    @Autowired private val subjectRepository: SubjectRepository) {

    data class AppStateRequest(
        val appChange: AuditEvent
    )

    @PostMapping("/analytics")
    fun getConfigWithCacheSize(@RequestBody  page: PageDTO): ResponseEntity<*> {
        val user = userService.getUserWithAuthorities()
        val subject = subjectService.findOneByLogin(user?.login)

        subject.id?.let {
            analyticsService.create(page, it)
            return ResponseEntity.ok(true)
        }

        return ResponseEntity.badRequest().body("Subject ID is missing")
    }

    @PostMapping("/analytics/appstate")
    fun postAppState(@RequestBody appStateRequest: AppStateRequest): ResponseEntity<*> {
        val user = userService.getUserWithAuthorities()
        val subject = subjectService.findOneByLogin(user?.login)

        subject.id?.let  {

            analyticsService.createAppChange(appStateRequest.appChange, it)
            return ResponseEntity.ok(true)
        }

        return ResponseEntity.badRequest().body("Subject ID or app state is missing")

    }

    @GetMapping("/analytics/subject/{login:" + Constants.ENTITY_ID_REGEX + "}")
    fun getAnalyticsForSubject(@PathVariable login: String): ResponseEntity<Any> {
        val subject = subjectService.findOneByLogin(login)

        val result = analyticsService.getEngagementForParticipant(subject)


        return ResponseEntity.ok(result)
    }
    companion object {
        private val logger = LoggerFactory.getLogger(AnalyticsResource::class.java)
    }

}



