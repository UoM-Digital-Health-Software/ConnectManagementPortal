package org.radarbase.management.web.rest

import org.springframework.boot.test.mock.mockito.MockBean



import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.radarbase.auth.authentication.OAuthHelper
import org.radarbase.auth.token.RadarToken
import org.radarbase.management.config.BasePostgresIntegrationTest
import org.radarbase.management.domain.*
import org.radarbase.management.domain.enumeration.*
import org.radarbase.management.repository.*
import org.radarbase.management.security.RadarAuthentication
import org.radarbase.management.service.*
import org.radarbase.management.service.dto.*
import org.radarbase.management.web.rest.errors.ExceptionTranslator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.web.PageableHandlerMethodArgumentResolver
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.mock.web.MockFilterConfig
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import javax.servlet.ServletException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.hamcrest.Matchers.not
import org.springframework.test.web.servlet.post
import java.security.Principal


internal class DeviceResourceIntTest(
    @Autowired private val deviceResource : DeviceResource,
    @Autowired private val pageableArgumentResolver: PageableHandlerMethodArgumentResolver,
    @Autowired private val jacksonMessageConverter: MappingJackson2HttpMessageConverter,
    @Autowired private val exceptionTranslator: ExceptionTranslator,
    @Autowired private val radarToken: RadarToken,
    @Autowired private val passwordService: PasswordService,
    @Autowired private val queryRepository: QueryRepository,
    @Autowired private val queryGroupRepository: QueryGroupRepository,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val subjectRepository: SubjectRepository,
    @Autowired private val queryParticipantRepository: QueryParticipantRepository,
    @Autowired private val queryLogicRepository: QueryLogicRepository,
    @Autowired private val queryEvaluationRepository: QueryEvaluationRepository,
    @Autowired private val queryContentRepository: QueryContentRepository,
    @Autowired private val queryContentService: QueryContentService,
    @Autowired private val queryParticipantContentRepository: QueryParticipantContentRepository,
    @Autowired private val queryContentGroupRepository: QueryContentGroupRepository

) : BasePostgresIntegrationTest() {

    private lateinit var mockMvc: MockMvc
    private val objectMapper = ObjectMapper()
    @Autowired private lateinit var mockUserService: UserService
    @Autowired private  lateinit  var queryBuilderService: QueryBuilderService

    @Autowired private lateinit var deviceRepository: DeviceRepository
    @MockBean
    private lateinit var notificationService: NotificationService
    private val imageBlob = "/9j/4AAQSkZJRgABAQAAAQABAAD/4gHYSUNDX1BST0ZJTEUAAQEAAAHIAAAAAAQwAABtbnRyUkdCIFhZWiAH4AABAAEAAAAAAABhY3NwAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAQAA9tYAAQAAAADTLQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAlkZXNjAAAA8AAAACRyWFlaAAABFAAAABRnWFlaAAABKAAAABRiWFlaAAABPAAAABR3dHB0AAABUAAAABRyVFJDAAABZAAAAChnVFJDAAABZAAAAChiVFJDAAABZAAAAChjcHJ0AAABjAAAADxtbHVjAAAAAAAAAAEAAAAMZW5VUwAAAAgAAAAcAHMAUgBHAEJYWVogAAAAAAAAb6IAADj1AAADkFhZWiAAAAAAAABimQAAt4UAABjaWFlaIAAAAAAAACSgAAAPhAAAts9YWVogAAAAAAAA9tYAAQAAAADTLXBhcmEAAAAAAAQAAAACZmYAAPKnAAANWQAAE9AAAApbAAAAAAAAAABtbHVjAAAAAAAAAAEAAAAMZW5VUwAAACAAAAAcAEcAbwBvAGcAbABlACAASQBuAGMALgAgADIAMAAxADb/2wBDAAoHBwgHBgoICAgLCgoLDhgQDg0NDh0VFhEYIx8lJCIfIiEmKzcvJik0KSEiMEExNDk7Pj4+JS5ESUM8SDc9Pjv/2wBDAQoLCw4NDhwQEBw7KCIoOzs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozv/wAARCAAUABQDASIAAhEBAxEB/8QAGgABAAIDAQAAAAAAAAAAAAAAAAMEAQIFB//EACQQAAICAgEDBAMAAAAAAAAAAAECAwQAIRESMUEFExQiUVJh/8QAFwEAAwEAAAAAAAAAAAAAAAAAAAECA//EABkRAQEBAAMAAAAAAAAAAAAAAAABESExQf/aAAwDAQACEQMRAD8A9hsCc15BWZFm6foZASoP94ytV9Qa1ZWOONXi9kO8wfjhidAL3IOzz21klupWlb5EyMWSNkBVyD0nuNHzlCG6ayWbl1pBHS5id2hKAKACXH7D8ka1oDfNeM7cvNx2cZpFIk0SSxsGR1DKw8g9sZLRHap17qIliISLHIsigk6ZTyDkxAIII5B8YxgWTtnGMYG//9k="

    private val baseURL = "/api/"

    private lateinit var user: User

    @BeforeEach
    @Throws(ServletException::class)

    fun setUp() {
        MockitoAnnotations.openMocks(this)

        mockUserService = mock()
        queryBuilderService = mock()


        val filter = OAuthHelper.createAuthenticationFilter()
        filter.init(MockFilterConfig())

        SecurityContextHolder.getContext().authentication = RadarAuthentication(radarToken)




        mockMvc = MockMvcBuilders.standaloneSetup(deviceResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setMessageConverters(jacksonMessageConverter)
            .addFilter<StandaloneMockMvcBuilder>(filter)
            .defaultRequest<StandaloneMockMvcBuilder>(MockMvcRequestBuilders.get("/").with(OAuthHelper.bearerToken()))
            .build()
    }









    @Test
    fun `POST device - should register device when deviceDTO is valid`() {
        val deviceDTO = DeviceDTO(/* fill required fields */)



        val sizeBefore = deviceRepository.findAll().size


        deviceDTO.token ="token"
        deviceDTO.platform = PlatformType.IOS


        mockMvc.post("/api/device") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(deviceDTO)

        }.andExpect {
            status { isOk() }
        }


        val sizeAfter = deviceRepository.findAll().size

        Assertions.assertThat(sizeAfter).isEqualTo(sizeBefore + 1)
    }




}
