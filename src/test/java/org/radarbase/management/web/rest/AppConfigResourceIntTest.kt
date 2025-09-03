package org.radarbase.management.web.rest

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.MockitoAnnotations
import org.radarbase.auth.authentication.OAuthHelper
import org.radarbase.management.ManagementPortalTestApp
import org.radarbase.management.domain.AppConfig
import org.radarbase.management.repository.SubjectRepository
import org.radarbase.management.service.*

import org.radarbase.management.service.mapper.SubjectMapper
import org.radarbase.management.web.rest.errors.ExceptionTranslator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.web.PageableHandlerMethodArgumentResolver
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.mock.web.MockFilterConfig
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder
import org.springframework.transaction.annotation.Transactional

import java.util.*
import javax.servlet.ServletException

/**
 * Test class for the SubjectResource REST controller.
 *
 * @see SubjectResource
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [ManagementPortalTestApp::class])
@WithMockUser
internal class AppConfigResourceIntTest(
    @Autowired private val appConfigResource: AppConfigResource,
    @Autowired private val subjectService: SubjectService,
    @Autowired private val jacksonMessageConverter: MappingJackson2HttpMessageConverter,
    @Autowired private val pageableArgumentResolver: PageableHandlerMethodArgumentResolver,
    @Autowired private val exceptionTranslator: ExceptionTranslator,
    @Autowired var objectMapper: ObjectMapper
) {
    private lateinit var restAppConfigResourceMockMvc: MockMvc

    @BeforeEach
    @Throws(ServletException::class)
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        val filter = OAuthHelper.createAuthenticationFilter()
        filter.init(MockFilterConfig())
        restAppConfigResourceMockMvc =
            MockMvcBuilders.standaloneSetup(appConfigResource).setCustomArgumentResolvers(pageableArgumentResolver)
                .setControllerAdvice(exceptionTranslator).setMessageConverters(jacksonMessageConverter)
                .addFilter<StandaloneMockMvcBuilder>(filter) // add the oauth token by default to all requests for this mockMvc
                .defaultRequest<StandaloneMockMvcBuilder>(
                    MockMvcRequestBuilders.get("/").with(OAuthHelper.bearerToken())
                ).build()
    }


    @Throws(Exception::class)
    @Transactional
    @Test
    fun subject() {
        val subjectDto = subjectService.createSubject(SubjectServiceTest.createEntityDTO())

        val result = restAppConfigResourceMockMvc.perform(MockMvcRequestBuilders.get("/api/config/null/null"))
            .andExpect(MockMvcResultMatchers.status().isOk()).andReturn()

        val json = result.response.contentAsString
        val configMap: Map<String, AppConfig> =
            objectMapper.readValue(json, object : TypeReference<Map<String, AppConfig>>() {})

        assertThat(configMap).isNotEmpty
        assertThat(configMap).containsKey("healthkitUpload.enabled")
        assertThat(configMap["healthkitUpload.enabled"]?.value).isEqualTo("true")
        assertThat(configMap["healthkitUpload.enabled"]?.type).isEqualTo("bool")

    }
}
