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
import org.radarbase.management.domain.enumeration.AppConfigType
import org.radarbase.management.repository.AppConfigRepository
import org.radarbase.management.repository.SubjectRepository
import org.radarbase.management.security.JwtAuthenticationFilter.Companion.radarToken
import org.radarbase.management.service.*

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockFilterConfig
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

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
    @Autowired private val subjectService: SubjectService,
    @Autowired var objectMapper: ObjectMapper,
    ) {
    private lateinit var restAppConfigResourceMockMvc: MockMvc
    @Autowired private lateinit var mockUserService: UserService
    @Autowired private lateinit var mockSubjectRepository: SubjectRepository
    @Autowired private lateinit var appConfigService: AppConfigService
    @Autowired private lateinit var appConfigRepository: AppConfigRepository

    @BeforeEach
    @Throws(ServletException::class)
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        mockUserService = mock()
        mockSubjectRepository = mock()

        appConfigRepository = mock()
        appConfigService = AppConfigService(appConfigRepository)

        var appConfigResource = AppConfigResource(appConfigService, mockUserService, mockSubjectRepository)

        val filter = OAuthHelper.createAuthenticationFilter()
        filter.init(MockFilterConfig())

        restAppConfigResourceMockMvc =
                MockMvcBuilders.standaloneSetup(appConfigResource).build()
    }


    private fun setupUser() : RadarToken {
        val token = mock<RadarToken>()
        val roles: MutableSet<Role> = HashSet()
        val role = Role()
        val authority = Authority()
        authority.name = RoleAuthority.SYS_ADMIN.authority
        role.authority = authority
        roles.add(role)

        val user = User()
        user.id = 1
        user.setLogin("93d21b93-1c1e-4aaf-983e-5b0cb4ae31f3")
        user.firstName = "john"
        user.lastName = "doe"
        user.email = "john.doe@jhipster.com"
        user.langKey = "en"
        user.roles = roles
        whenever(mockUserService.getUserWithAuthorities()).doReturn(user)


        return token;
    }

    @Throws(Exception::class)
    @Transactional
    @Test
    fun subject() {
        var appConfig = AppConfig()
        appConfig.key  = "healthkitUpload.enabled"
        appConfig.type = AppConfigType.bool
        appConfig.rolloutPct = 100
        appConfig.rolloutVersion = "v1"
        appConfig.value = "true"
        appConfig.createdAt = Instant.now()

        whenever(appConfigRepository.findBySiteIsNullAndUserIdIsNull())
            .thenReturn(listOf(appConfig))

        val token = setupUser()

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
    }

    @Throws(Exception::class)
    @Transactional
    @Test
    fun postAppConfig() {
        val token = setupUser()

        var appConfig = AppConfig()
        appConfig.key  = "healthkitUpload.enabled"
        appConfig.type = AppConfigType.bool
        appConfig.rolloutPct = 100
        appConfig.rolloutVersion = "v1"
        appConfig.value = "true"
        appConfig.createdAt = Instant.now()
        appConfig.conditional = "#cacheSize > 100"

        whenever(appConfigRepository.findBySiteIsNullAndUserIdIsNull())
            .thenReturn(listOf(appConfig))

        val subjectDto = subjectService.createSubject(SubjectServiceTest.createEntityDTO())


        val jsonBody = """
            {
                "cacheSize": 101
            }
      """.trimIndent()


        var result = restAppConfigResourceMockMvc.perform(
            MockMvcRequestBuilders.post("/api/app-config/healthkitUpload.enabled")
                .contentType("application/json")
                .content(jsonBody)
                .with { request ->
                    request.radarToken = token
                    request.remoteUser = "test"
                    request
                }
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn()

        val json = result.response.contentAsString
        val configMap: Boolean =
            objectMapper.readValue(json, object : TypeReference<Boolean>() {})

        assertThat(configMap).isEqualTo(true)


        val jsonBody1 = """
            {
                "cacheSize": 100
            }
        """.trimIndent()
        result = restAppConfigResourceMockMvc.perform(
            MockMvcRequestBuilders.post("/api/app-config/healthkitUpload.enabled")
                .contentType("application/json")
                .content(jsonBody1)
                .with { request ->
                    request.radarToken = token
                    request.remoteUser = "test"
                    request
                }
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn()

        val json1 = result.response.contentAsString
        val configMap1: Boolean =
            objectMapper.readValue(json1, object : TypeReference<Boolean>() {})

        assertThat(configMap1).isEqualTo(false)


        result = restAppConfigResourceMockMvc.perform(
            MockMvcRequestBuilders.post("/api/app-config/healthkitUpload.enabled") // your POST endpoint
                .contentType("application/json")
                .with { request ->
                    request.radarToken = token
                    request.remoteUser = "test"
                    request
                }
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn()

        val json2 = result.response.contentAsString
        val configMap2: Boolean =
            objectMapper.readValue(json1, object : TypeReference<Boolean>() {})

        assertThat(configMap1).isEqualTo(false)


    }
}
