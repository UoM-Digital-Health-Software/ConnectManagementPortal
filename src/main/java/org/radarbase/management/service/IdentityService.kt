package org.radarbase.management.service

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.radarbase.auth.authorization.Permission
import org.radarbase.auth.authorization.RoleAuthority
import org.radarbase.auth.exception.IdpException
import org.radarbase.auth.kratos.KratosSessionDTO
import org.radarbase.management.config.ManagementPortalProperties
import org.radarbase.management.domain.Role
import org.radarbase.management.domain.User
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration

/**
 * Service class for managing identities.
 */
@Service
@Transactional
class IdentityService(
    @Autowired private val managementPortalProperties: ManagementPortalProperties,
    @Autowired private val authService: AuthService
) {
    private val httpClient = HttpClient(CIO).config {
        install(HttpTimeout) {
            connectTimeoutMillis = Duration.ofSeconds(10).toMillis()
            socketTimeoutMillis = Duration.ofSeconds(10).toMillis()
            requestTimeoutMillis = Duration.ofSeconds(300).toMillis()
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }

    /** Save a [User] as to the IDP as an identity. Returns the generated [KratosSessionDTO.Identity] */
    @Throws(IdpException::class)
    suspend fun saveAsIdentity(user: User): KratosSessionDTO.Identity? {
        val kratosIdentity: KratosSessionDTO.Identity?

        withContext(Dispatchers.IO) {
            val identity = createIdentity(user)
            val response = httpClient.post {
                url("${managementPortalProperties.identityServer}/admin/identities")
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(identity)
            }


            if (response.status.isSuccess()) {
                kratosIdentity = response.body<KratosSessionDTO.Identity>()
                log.debug("saved identity for user ${user.login} to IDP as ${kratosIdentity.id}")
            } else {
                throw IdpException(
                    "couldn't save Kratos ID to server at " + managementPortalProperties.identityServer.serverUrl
                )
            }
        }

        return kratosIdentity
    }

    /**
     * Convert a [User] to a [KratosSessionDTO.Identity] object.
     * @param user The object to convert
     * @return the newly created DTO object
     */
    @Throws(IdpException::class)
    fun createIdentity(user: User): KratosSessionDTO.Identity {
        try {
            return KratosSessionDTO.Identity(
                schema_id = "subject",
                traits = KratosSessionDTO.Traits(email = user.email),
                metadata_public = KratosSessionDTO.Metadata(
                    aud = emptyList(),
                    sources = emptyList(), //empty at the time of creation
                    roles = user.roles.mapNotNull { role: Role ->
                        val auth = role.authority?.name
                        when (role.role?.scope) {
                            RoleAuthority.Scope.GLOBAL -> auth
                            RoleAuthority.Scope.ORGANIZATION -> role.organization!!.name + ":" + auth
                            RoleAuthority.Scope.PROJECT -> role.project!!.projectName + ":" + auth
                            null -> null
                        }
                    }.toList(),
                    authorities = user.authorities,
                    scope = Permission.scopes().filter { scope ->
                        val permission = Permission.ofScope(scope)
                        val auths = user.roles.mapNotNull { it.role }

                        return@filter authService.mayBeGranted(auths, permission)
                    },
                    mp_login = user.login
                )
            )
        }
        catch (e: Throwable){
            val message = "could not convert user ${user.login} to identity"
            log.error(message)
            throw IdpException(message, e)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(IdentityService::class.java)
    }
}
