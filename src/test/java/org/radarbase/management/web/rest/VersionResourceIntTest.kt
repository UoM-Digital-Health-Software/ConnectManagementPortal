package org.radarbase.management.web.rest

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.mock
import org.radarbase.auth.authentication.OAuthHelper
import org.radarbase.auth.token.RadarToken
import org.radarbase.management.config.BasePostgresIntegrationTest
import org.radarbase.management.config.ManagementPortalProperties
import org.radarbase.management.security.RadarAuthentication
import org.radarbase.management.service.UserService
import org.radarbase.management.web.rest.errors.ExceptionTranslator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.web.PageableHandlerMethodArgumentResolver
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.mock.web.MockFilterConfig
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder
import javax.servlet.ServletException


internal class VersionResourceIntTest (
    @Autowired private val versionResource : VersionRescource,
    @Autowired private val pageableArgumentResolver: PageableHandlerMethodArgumentResolver,
    @Autowired private val jacksonMessageConverter: MappingJackson2HttpMessageConverter,
    @Autowired private val exceptionTranslator: ExceptionTranslator,
    @Autowired private val radarToken: RadarToken,

    ): BasePostgresIntegrationTest() {

    private lateinit var mockMvc: MockMvc
    private val objectMapper = ObjectMapper()
    @Autowired
    private lateinit var mockUserService: UserService

    private val baseURL = "/api/"


    @BeforeEach
    @Throws(ServletException::class)

    fun setUp() {
        MockitoAnnotations.openMocks(this)

        mockUserService = mock()

        val filter = OAuthHelper.createAuthenticationFilter()
        filter.init(MockFilterConfig())

        SecurityContextHolder.getContext().authentication = RadarAuthentication(radarToken)


        mockMvc = MockMvcBuilders.standaloneSetup(versionResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setMessageConverters(jacksonMessageConverter)
            .addFilter<StandaloneMockMvcBuilder>(filter)
            .defaultRequest<StandaloneMockMvcBuilder>(MockMvcRequestBuilders.get("/").with(OAuthHelper.bearerToken()))
            .build()
    }

    @Test
    @Throws(Exception::class)
    fun testGetMinRequiredVersion(): Unit {

        mockMvc.perform(get(baseURL + "min-required-version"))
            .andExpect(status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.ios").value("1.0.0"))
            .andExpect(jsonPath("$.android").value("1.0.0"))


    }
}
