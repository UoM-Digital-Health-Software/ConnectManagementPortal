package org.radarbase.management.web.rest

import org.radarbase.management.service.CBTContentService
import org.radarbase.management.service.CBTContentType
import org.radarbase.management.service.catalog.CBTContent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api/cbtcontent")
class CBTContentController(@Autowired val cbtContentService: CBTContentService) {


    @GetMapping("/exercises/{type}")
    fun getExercises(@PathVariable type: CBTContentType): List<CBTContent> {
        return cbtContentService.getContentFor(type)
    }




}
