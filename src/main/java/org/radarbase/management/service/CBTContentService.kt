package org.radarbase.management.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.radarbase.management.service.catalog.CBTContent
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import com.fasterxml.jackson.core.type.TypeReference
import org.radarbase.management.web.rest.QueryResource
import org.slf4j.LoggerFactory


enum class CBTContentType(val fileName: String) {
    CANNABIS("cannabis.json"),
    FEELING_CRITICISED("feeling_criticised.json"),
    SLEEP("sleep.json"),
    SUSPICIOUS_THOUGHTS("suspicious_thoughts.json"),
    VOICES("voices.json"),
    GETTING_OUT("getting_out.json")
}

@Service
class CBTContentService(private val objectMapper: ObjectMapper) {

    private var cbtContent: List<CBTContent> = emptyList()

    fun getContentFor(name: CBTContentType) :  List<CBTContent> {
        val resource = ClassPathResource("content/" + name.fileName)
        resource.inputStream.use {
            inputStream ->
            cbtContent = objectMapper.readValue(inputStream, object: TypeReference<List<CBTContent>>(){})

            return cbtContent
        }
    }

    fun getExercises(): List<CBTContent> = cbtContent


    companion object {
        private val log = LoggerFactory.getLogger(CBTContentService::class.java)
    }
}
