package org.radarbase.management.web.rest

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.hamcrest.Matchers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.MockitoAnnotations
import org.radarbase.auth.authentication.OAuthHelper
import org.radarbase.auth.authorization.RoleAuthority
import org.radarbase.management.ManagementPortalTestApp
import org.radarbase.management.domain.Authority
import org.radarbase.management.domain.Project
import org.radarbase.management.domain.Role
import org.radarbase.management.domain.User
import org.radarbase.management.repository.ProjectRepository
import org.radarbase.management.repository.RoleRepository
import org.radarbase.management.repository.UserRepository
import org.radarbase.management.service.PasswordService
import org.radarbase.management.service.UserService
import org.radarbase.management.service.UserServiceIntTest
import org.radarbase.management.service.dto.RoleDTO
import org.radarbase.management.web.rest.errors.ExceptionTranslator
import org.radarbase.management.web.rest.vm.ManagedUserVM
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.web.PageableHandlerMethodArgumentResolver
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.mock.web.MockFilterConfig
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder
import org.springframework.transaction.annotation.Transactional
import java.util.stream.Collectors
import javax.servlet.ServletException

/**
 * Test class for the UserResource REST controller.
 *
 * @see UserResource
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [ManagementPortalTestApp::class])
@WithMockUser
internal class UserResourceIntTest(
    @Autowired private val userService: UserService,
    @Autowired private val userResource: UserResource,

    @Autowired private val roleRepository: RoleRepository,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val jacksonMessageConverter: MappingJackson2HttpMessageConverter,
    @Autowired private val pageableArgumentResolver: PageableHandlerMethodArgumentResolver,
    @Autowired private val exceptionTranslator: ExceptionTranslator,
    @Autowired private val passwordService: PasswordService,
    @Autowired private val projectRepository: ProjectRepository,
) {
    private lateinit var restUserMockMvc: MockMvc
    private lateinit var user: User
    private var project: Project? = null

    @BeforeEach
    @Throws(ServletException::class)
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        val filter = OAuthHelper.createAuthenticationFilter()
        filter.init(MockFilterConfig())
        restUserMockMvc = MockMvcBuilders.standaloneSetup(userResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setMessageConverters(jacksonMessageConverter)
            .addFilter<StandaloneMockMvcBuilder>(filter)
            .defaultRequest<StandaloneMockMvcBuilder>(MockMvcRequestBuilders.get("/").with(OAuthHelper.bearerToken()))
            .build()
        project = null
    }

    @AfterEach
    fun tearDown() {
        runBlocking {
            // delete any leftover users from previous tests
            try {
                userService.deleteUser(user.login!!)
                userService.deleteUser(UserServiceIntTest.UPDATED_LOGIN)
            } catch (e: Exception) {
                // ignore
            }
        }

        if (project != null) {
            projectRepository.delete(project!!)
        }
    }

    @BeforeEach
    fun initTest() {
        runBlocking {
            user = UserServiceIntTest.createEntity(passwordService)

            // delete any leftover users from previous tests
            try {
                userService.deleteUser(user.login!!)
                userService.deleteUser(UserServiceIntTest.UPDATED_LOGIN)
            } catch (e: Exception) {
                // ignore
            }

            val roles = roleRepository
                .findRolesByAuthorityName(RoleAuthority.PARTICIPANT.authority)
                .stream().filter { r: Role -> r.project == null }
                .collect(Collectors.toList())
            roleRepository.deleteAll(roles)
        }
    }

//    @Test
//    @Transactional
//    @Throws(Exception::class)
//    fun createUser() {
//        val databaseSizeBeforeCreate = userRepository.findAll().size
//
//        // Create the User
//        val roles: MutableSet<RoleDTO> = HashSet()
//        val role = RoleDTO()
//        role.authorityName = RoleAuthority.SYS_ADMIN_AUTHORITY
//        roles.add(role)
//        val managedUserVm = createDefaultUser(roles)
//        val result = restUserMockMvc.perform(
//            MockMvcRequestBuilders.post("/api/users")
//                .contentType(TestUtil.APPLICATION_JSON_UTF8)
//                .content(TestUtil.convertObjectToJsonBytes(managedUserVm))
//        ).andReturn()
//
//        restUserMockMvc.perform(asyncDispatch(result))
//            .andExpect(MockMvcResultMatchers.status().isCreated())
//
//        // Validate the User in the database
//        val userList = userRepository.findAll()
//        Assertions.assertThat(userList).hasSize(databaseSizeBeforeCreate + 1)
//        val testUser = userList[userList.size - 1]
//        Assertions.assertThat(testUser.login).isEqualTo(UserServiceIntTest.DEFAULT_LOGIN)
//        Assertions.assertThat(testUser.firstName).isEqualTo(UserServiceIntTest.DEFAULT_FIRSTNAME)
//        Assertions.assertThat(testUser.lastName).isEqualTo(UserServiceIntTest.DEFAULT_LASTNAME)
//        Assertions.assertThat(testUser.email).isEqualTo(UserServiceIntTest.DEFAULT_EMAIL)
//        Assertions.assertThat(testUser.langKey).isEqualTo(UserServiceIntTest.DEFAULT_LANGKEY)
//    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun createUserWithExistingId(): Unit = runBlocking{
        val databaseSizeBeforeCreate = userRepository.findAll().size
        val roles: MutableSet<RoleDTO> = HashSet()
        val role = RoleDTO()
        role.authorityName = RoleAuthority.PARTICIPANT.authority
        roles.add(role)
        val managedUserVm = createDefaultUser(roles)
        managedUserVm.id = 1L

        // An entity with an existing ID cannot be created, so this API call must fail
        val result = restUserMockMvc.perform(
            MockMvcRequestBuilders.post("/api/users")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(managedUserVm))
        ).andReturn()

        restUserMockMvc.perform(asyncDispatch(result))
            .andExpect(MockMvcResultMatchers.status().isBadRequest())

        // Validate the User in the database
        val userList = userRepository.findAll()
        Assertions.assertThat(userList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun createUserWithExistingLogin(): Unit = runBlocking {
        // Initialize the database
        userRepository.saveAndFlush(user)
        val databaseSizeBeforeCreate = userRepository.findAll().size
        val roles: MutableSet<RoleDTO> = HashSet()
        val role = RoleDTO()
        role.authorityName = RoleAuthority.PARTICIPANT.authority
        roles.add(role)
        val managedUserVm = createDefaultUser(roles)
        managedUserVm.email = "anothermail@localhost"

        // Create the User
        val result = restUserMockMvc.perform(
            MockMvcRequestBuilders.post("/api/users")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(managedUserVm))
        ).andReturn()

        restUserMockMvc.perform(asyncDispatch(result))
            .andExpect(MockMvcResultMatchers.status().isBadRequest())

        // Validate the User in the database
        val userList = userRepository.findAll()
        Assertions.assertThat(userList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun createUserWithExistingEmail(): Unit = runBlocking {
        // Initialize the database
        userRepository.saveAndFlush(user)
        val databaseSizeBeforeCreate = userRepository.findAll().size
        val roles: MutableSet<RoleDTO> = HashSet()
        val role = RoleDTO()
        role.authorityName = RoleAuthority.PARTICIPANT.authority
        roles.add(role)
        val managedUserVm = createDefaultUser(roles)
        managedUserVm.login = "anotherlogin"

        // Create the User
        val result = restUserMockMvc.perform(
            MockMvcRequestBuilders.post("/api/users")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(managedUserVm))
        ).andReturn()


        restUserMockMvc.perform(asyncDispatch(result))
            .andExpect(MockMvcResultMatchers.status().isBadRequest())

        // Validate the User in the database
        val userList = userRepository.findAll()
        Assertions.assertThat(userList).hasSize(databaseSizeBeforeCreate)
    }

    @Throws(Exception::class)
    @Transactional
    @Test
    fun getAllUsers() {
        // Initialize the database
        val adminRole = Role()
        adminRole.id = 1L
        adminRole.authority = Authority(RoleAuthority.SYS_ADMIN)
        adminRole.project = null
        val userWithRole = User()
        userWithRole.setLogin(UserServiceIntTest.DEFAULT_LOGIN)
        userWithRole.password = passwordService.generateEncodedPassword()
        userWithRole.activated = true
        userWithRole.email = UserServiceIntTest.DEFAULT_EMAIL
        userWithRole.firstName = UserServiceIntTest.DEFAULT_FIRSTNAME
        userWithRole.lastName = UserServiceIntTest.DEFAULT_LASTNAME
        userWithRole.langKey = UserServiceIntTest.DEFAULT_LANGKEY
        userWithRole.roles = mutableSetOf(adminRole)
        userRepository.saveAndFlush(userWithRole)

        // Get all the users
        restUserMockMvc.perform(
            MockMvcRequestBuilders.get("/api/users?sort=id,desc")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.[*].login")
                    .value<Iterable<String?>>(Matchers.hasItem(UserServiceIntTest.DEFAULT_LOGIN))
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.[*].firstName")
                    .value<Iterable<String?>>(Matchers.hasItem(UserServiceIntTest.DEFAULT_FIRSTNAME))
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.[*].lastName")
                    .value<Iterable<String?>>(Matchers.hasItem(UserServiceIntTest.DEFAULT_LASTNAME))
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.[*].email")
                    .value<Iterable<String?>>(Matchers.hasItem(UserServiceIntTest.DEFAULT_EMAIL))
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.[*].langKey")
                    .value<Iterable<String?>>(Matchers.hasItem(UserServiceIntTest.DEFAULT_LANGKEY))
            )
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getUser() {
        // Initialize the database
        userRepository.saveAndFlush(user)

        // Get the user
        restUserMockMvc.perform(MockMvcRequestBuilders.get("/api/users/{login}", user.login))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.jsonPath("$.login").value(user.login))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.firstName").value(UserServiceIntTest.DEFAULT_FIRSTNAME)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.lastName").value(UserServiceIntTest.DEFAULT_LASTNAME)
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.email").value(UserServiceIntTest.DEFAULT_EMAIL))
            .andExpect(MockMvcResultMatchers.jsonPath("$.langKey").value(UserServiceIntTest.DEFAULT_LANGKEY))
    }

    @Throws(Exception::class)
    @Transactional
    @Test
    fun getNonExistingUser() {
        restUserMockMvc.perform(MockMvcRequestBuilders.get("/api/users/unknown"))
            .andExpect(MockMvcResultMatchers.status().isNotFound())
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun updateUser() {
        // Initialize the database
        userRepository.saveAndFlush(user)
        val databaseSizeBeforeUpdate = userRepository.findAll().size
        project = ProjectResourceIntTest.createEntity()
        projectRepository.save(project!!)

        // Update the user
        val updatedUser = userRepository.findById(user.id!!)
            .orElseThrow { AssertionError("Cannot find user " + user.id) }
        val managedUserVm = ManagedUserVM()
        managedUserVm.id = updatedUser.id
        managedUserVm.login = updatedUser.login
        managedUserVm.password = UserServiceIntTest.UPDATED_PASSWORD
        managedUserVm.firstName = UserServiceIntTest.UPDATED_FIRSTNAME
        managedUserVm.lastName = UserServiceIntTest.UPDATED_LASTNAME
        managedUserVm.email = UserServiceIntTest.UPDATED_EMAIL
        managedUserVm.isActivated = updatedUser.activated
        managedUserVm.langKey = UserServiceIntTest.UPDATED_LANGKEY
        val role = RoleDTO()
        role.projectId = project!!.id
        role.authorityName = RoleAuthority.PARTICIPANT.authority
        managedUserVm.roles = mutableSetOf(role)
        restUserMockMvc.perform(
            MockMvcRequestBuilders.put("/api/users")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(managedUserVm))
        )
            .andExpect(MockMvcResultMatchers.status().isOk())

        // Validate the User in the database
        val userList = userRepository.findAll()
        Assertions.assertThat(userList).hasSize(databaseSizeBeforeUpdate)
        val testUser = userList[userList.size - 1]
        Assertions.assertThat(testUser.firstName).isEqualTo(UserServiceIntTest.UPDATED_FIRSTNAME)
        Assertions.assertThat(testUser.lastName).isEqualTo(UserServiceIntTest.UPDATED_LASTNAME)
        Assertions.assertThat(testUser.email).isEqualTo(UserServiceIntTest.UPDATED_EMAIL)
        Assertions.assertThat(testUser.langKey).isEqualTo(UserServiceIntTest.UPDATED_LANGKEY)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun updateUserLogin() {
        // Initialize the database
        userRepository.saveAndFlush(user)
        project = ProjectResourceIntTest.createEntity()
        projectRepository.save(project!!)
        val databaseSizeBeforeUpdate = userRepository.findAll().size

        // Update the user
        val updatedUser = userRepository.findById(user.id!!)
            .orElseThrow { AssertionError("Cannot find user " + user.id) }
        val managedUserVm = ManagedUserVM()
        managedUserVm.id = updatedUser.id
        managedUserVm.login = UserServiceIntTest.UPDATED_LOGIN
        managedUserVm.password = UserServiceIntTest.UPDATED_PASSWORD
        managedUserVm.firstName = UserServiceIntTest.UPDATED_FIRSTNAME
        managedUserVm.lastName = UserServiceIntTest.UPDATED_LASTNAME
        managedUserVm.email = UserServiceIntTest.UPDATED_EMAIL
        managedUserVm.isActivated = updatedUser.activated
        managedUserVm.langKey = UserServiceIntTest.UPDATED_LANGKEY
        val role = RoleDTO()
        role.projectId = project!!.id
        role.authorityName = RoleAuthority.PARTICIPANT.authority
        managedUserVm.roles = mutableSetOf(role)
        restUserMockMvc.perform(
            MockMvcRequestBuilders.put("/api/users")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(managedUserVm))
        )
            .andExpect(MockMvcResultMatchers.status().isOk())

        // Validate the User in the database
        val userList = userRepository.findAll()
        Assertions.assertThat(userList).hasSize(databaseSizeBeforeUpdate)
        val testUser = userList[userList.size - 1]
        Assertions.assertThat(testUser.login).isEqualTo(UserServiceIntTest.DEFAULT_LOGIN)
        Assertions.assertThat(testUser.firstName).isEqualTo(UserServiceIntTest.UPDATED_FIRSTNAME)
        Assertions.assertThat(testUser.lastName).isEqualTo(UserServiceIntTest.UPDATED_LASTNAME)
        Assertions.assertThat(testUser.email).isEqualTo(UserServiceIntTest.UPDATED_EMAIL)
        Assertions.assertThat(testUser.langKey).isEqualTo(UserServiceIntTest.UPDATED_LANGKEY)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun updateUserExistingEmail() = runBlocking {
        // Initialize the database with 2 users
        userRepository.saveAndFlush(user)
        project = ProjectResourceIntTest.createEntity()
        projectRepository.save(project!!)
        val anotherUser = User()
        anotherUser.setLogin("jhipster")
        anotherUser.password = passwordService.generateEncodedPassword()
        anotherUser.activated = true
        anotherUser.email = "jhipster@localhost"
        anotherUser.firstName = "java"
        anotherUser.lastName = "hipster"
        anotherUser.langKey = "en"
        userRepository.saveAndFlush(anotherUser)

        // Update the user
        val updatedUser = userRepository.findById(user.id!!)
            .orElseThrow { AssertionError("Cannot find user " + user.id) }
        val managedUserVm = ManagedUserVM()
        managedUserVm.id = updatedUser.id
        managedUserVm.login = updatedUser.login
        managedUserVm.password = updatedUser.password
        managedUserVm.firstName = updatedUser.firstName
        managedUserVm.lastName = updatedUser.lastName
        managedUserVm.email = "jhipster@localhost"
        managedUserVm.isActivated = updatedUser.activated
        managedUserVm.langKey = updatedUser.langKey
        val role = RoleDTO()
        role.projectId = project!!.id
        role.authorityName = RoleAuthority.PARTICIPANT.authority
        managedUserVm.roles = setOf(role)
        val result = restUserMockMvc.perform(
            MockMvcRequestBuilders.put("/api/users")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(managedUserVm))
        ).andReturn()

        restUserMockMvc.perform(asyncDispatch(result))
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun updateUserExistingLogin(): Unit = runBlocking {
        // Initialize the database
        userRepository.saveAndFlush(user)
        project = ProjectResourceIntTest.createEntity()
        projectRepository.save(project!!)
        val anotherUser = User()
        anotherUser.setLogin("jhipster")
        anotherUser.password = passwordService.generateEncodedPassword()
        anotherUser.activated = true
        anotherUser.email = "jhipster@localhost"
        anotherUser.firstName = "java"
        anotherUser.lastName = "hipster"
        anotherUser.langKey = "en"
        userRepository.saveAndFlush(anotherUser)

        // Update the user
        val updatedUser = userRepository.findById(user.id!!)
            .orElseThrow { AssertionError("Cannot find user " + user.id) }
        val managedUserVm = ManagedUserVM()
        managedUserVm.id = updatedUser.id
        managedUserVm.login = "jhipster"
        managedUserVm.password = updatedUser.password
        managedUserVm.firstName = updatedUser.firstName
        managedUserVm.lastName = updatedUser.lastName
        managedUserVm.email = updatedUser.email
        managedUserVm.isActivated = updatedUser.activated
        managedUserVm.langKey = updatedUser.langKey
        val role = RoleDTO()
        role.projectId = project!!.id
        role.authorityName = RoleAuthority.PARTICIPANT.authority
        managedUserVm.roles = setOf(role)
        val result = restUserMockMvc.perform(
            MockMvcRequestBuilders.put("/api/users")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(managedUserVm))
        ).andReturn()

        restUserMockMvc.perform(asyncDispatch(result))
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun deleteUser(): Unit = runBlocking {
        // Initialize the database
        userRepository.saveAndFlush(user)
        val databaseSizeBeforeDelete = userRepository.findAll().size

        // Delete the user
        val result = restUserMockMvc.perform(
            MockMvcRequestBuilders.delete("/api/users/{login}", user.login)
                .accept(TestUtil.APPLICATION_JSON_UTF8)
        ).andReturn()

        result.getAsyncResult()

        restUserMockMvc.perform(asyncDispatch(result))
            .andExpect(MockMvcResultMatchers.status().isOk())

        // Validate the database is empty
        val userList = userRepository.findAll()
        Assertions.assertThat(userList).hasSize(databaseSizeBeforeDelete - 1)
    }

    @Test
    @Transactional
    fun equalsVerifier() {
        val userA = User()
        userA.setLogin("AAA")
        val userB = User()
        userB.setLogin("BBB")
        Assertions.assertThat(userA).isNotEqualTo(userB)
    }

    private fun createDefaultUser(roles: Set<RoleDTO>): ManagedUserVM {
        val result = ManagedUserVM()
        result.login = UserServiceIntTest.DEFAULT_LOGIN
        result.password = UserServiceIntTest.DEFAULT_PASSWORD
        result.firstName = UserServiceIntTest.DEFAULT_FIRSTNAME
        result.lastName = UserServiceIntTest.DEFAULT_LASTNAME
        result.isActivated = true
        result.email = UserServiceIntTest.DEFAULT_EMAIL
        result.langKey = UserServiceIntTest.DEFAULT_LANGKEY
        result.roles = roles
        return result
    }
}
