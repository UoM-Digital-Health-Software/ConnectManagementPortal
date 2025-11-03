package org.radarbase.management.web.rest

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.radarbase.auth.authentication.OAuthHelper
import org.radarbase.auth.authorization.RoleAuthority
import org.radarbase.auth.token.RadarToken
import org.radarbase.management.ManagementPortalTestApp
import org.radarbase.management.domain.AppConfig
import org.radarbase.management.domain.Authority
import org.radarbase.management.domain.Role
import org.radarbase.management.domain.User
import org.radarbase.management.repository.SubjectRepository
import org.radarbase.management.security.JwtAuthenticationFilter.Companion.radarToken
import org.radarbase.management.security.RadarAuthentication
import org.radarbase.management.service.*

import org.radarbase.management.service.mapper.SubjectMapper
import org.radarbase.management.web.rest.errors.ExceptionTranslator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.web.PageableHandlerMethodArgumentResolver
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.mock.web.MockFilterConfig
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.core.context.SecurityContextHolder
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
    @Autowired var objectMapper: ObjectMapper,
    @Autowired private val radarToken: RadarToken,
    @Autowired private val appConfigService: AppConfigService,
    @Autowired private val userService: UserService


    ) {
    private lateinit var restAppConfigResourceMockMvc: MockMvc
    @Autowired private lateinit var mockUserService: UserService
    @Autowired private lateinit var mockSubjectRepository: SubjectRepository

    @BeforeEach
    @Throws(ServletException::class)
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        mockUserService = mock()
        mockSubjectRepository = mock()

        var appConfigResource = AppConfigResource(appConfigService, mockUserService, mockSubjectRepository)

        val filter = OAuthHelper.createAuthenticationFilter()
        filter.init(MockFilterConfig())

        restAppConfigResourceMockMvc =
                MockMvcBuilders.standaloneSetup(appConfigResource).build()
    }


    @Throws(Exception::class)
    @Transactional
    @Test
    fun subject() {
        val token = mock<RadarToken>()
        val roles: MutableSet<Role> = HashSet()
        val role = Role()
        val authority = Authority()
        authority.name = RoleAuthority.SYS_ADMIN.authority
        role.authority = authority
        roles.add(role)

        val user = User()
        user.setLogin("93d21b93-1c1e-4aaf-983e-5b0cb4ae31f3")
        user.firstName = "john"
        user.lastName = "doe"
        user.email = "john.doe@jhipster.com"
        user.langKey = "en"
        user.roles = roles
        whenever(mockUserService.getUserWithAuthorities()).doReturn(user)



        val subjectDto = subjectService.createSubject(SubjectServiceTest.createEntityDTO())

        var result = restAppConfigResourceMockMvc.perform(MockMvcRequestBuilders.get("/api/app-config/healthkitUpload.enabled")            .with { request: MockHttpServletRequest ->
            request.radarToken = token
            request.remoteUser = "test"
            request
        })

            .andExpect(MockMvcResultMatchers.status().isOk()).andReturn()

        val json = result.response.contentAsString
        val configMap: Boolean =
            objectMapper.readValue(json, object : TypeReference<Boolean>() {})

        assertThat(configMap).isEqualTo(true)

        user.setLogin("ae3f7566-a2d6-44e6-9b62-a00cd8de439f")
        whenever(mockUserService.getUserWithAuthorities()).doReturn(user)


         result = restAppConfigResourceMockMvc.perform(MockMvcRequestBuilders.get("/api/app-config/healthkitUpload.enabled")            .with { request: MockHttpServletRequest ->
            request.radarToken = token
            request.remoteUser = "test"
            request
        })

            .andExpect(MockMvcResultMatchers.status().isOk()).andReturn()

        val json1 = result.response.contentAsString
        val configMap1: Boolean =
            objectMapper.readValue(json1, object : TypeReference<Boolean>() {})

        assertThat(configMap1).isEqualTo(false)


    }
}
