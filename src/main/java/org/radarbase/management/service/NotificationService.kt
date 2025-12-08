package org.radarbase.management.service

import com.google.firebase.messaging.*
import com.hazelcast.internal.util.StringUtil.shorten
import org.radarbase.management.domain.*
import org.radarbase.management.domain.enumeration.PlatformType
import org.radarbase.management.repository.ContentNotificationRepository
import org.radarbase.management.repository.DeviceRepository
import org.radarbase.management.repository.MetaTokenRepository
import org.radarbase.management.repository.UserRepository
import org.radarbase.management.service.dto.DeviceDTO
import org.radarbase.management.service.dto.NotificationDTO
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.*


@Service
@Transactional
class NotificationService(
    private val userRepository: UserRepository,
    private val deviceRepository: DeviceRepository,
    private val contentNotificationRepository: ContentNotificationRepository
    ) {

    @Autowired
    private val firebaseMessaging: FirebaseMessaging?
            = null


    private val MAXIMUM_TITLE_LENGTH = 65
    private val ANDROID_MAXIMUM_BODY_LENGTH = 240
    private val APPLE_MAXIMUM_BODY_LENGTH = 178


    fun registerDevice(deviceDTO: DeviceDTO?, login: String?) {
        val device = Device().apply {
            platform = deviceDTO?.platform
            token = deviceDTO?.token
        }

        val user: User? =  userRepository.findOneByLogin(login)

        user.let {
            val existingDevice: Optional<Device> =
                deviceRepository.findByUserAndToken(user, device.token)
            if (!existingDevice.isPresent()) {
                device.user = user
                deviceRepository.save(device)
            }
        }
    }


    private fun sendAppleNotifications(tokens: List<String>, title: String, body: String, route: String): Boolean {
        if (tokens.isEmpty()) {
            return false
        }


        val shortenedTitle = shorten(title, MAXIMUM_TITLE_LENGTH)
        val shortenedBody = shorten(body, APPLE_MAXIMUM_BODY_LENGTH)

        val apnsConfig = ApnsConfig.builder()
            .setAps(
                Aps.builder()
                    .setSound("default")
                    .build()
            )
            .build()

        val message = MulticastMessage.builder()
            .setApnsConfig(apnsConfig)
            .setNotification(
                Notification.builder()
                    .setTitle(shortenedTitle)
                    .setBody(shortenedBody)
                    .build()
            )
            .putData("route", route)
            .addAllTokens(tokens)
            .build()

        return try {
            val response = firebaseMessaging?.sendEachForMulticast(message)
            if (response != null) {
                processNotificationResponses(response.responses, tokens)
                response.successCount > 0
            } else false
        } catch (exception: FirebaseMessagingException) {
            log.error("[iOS] Error sending notification", exception)
            false
        }
    }

    private fun sendAndroidNotifications(tokens: List<String>, title: String, body: String, route: String): Boolean {
        var title: String? = title
        var body: String? = body
        if (tokens.isEmpty()) {
            return false
        }
        title = shorten(title, MAXIMUM_TITLE_LENGTH)
        body = shorten(body, ANDROID_MAXIMUM_BODY_LENGTH)

        val message = MulticastMessage.builder()
            .setAndroidConfig(
                AndroidConfig.builder()
                    .setNotification(
                        AndroidNotification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .setChannelId("high_importance_channel")
                            .build()
                    )
                    .putData("route", route)
                    .build()
            )
            .addAllTokens(tokens)
            .build()


        return try {
            val response = firebaseMessaging?.sendEachForMulticast(message)
            if (response != null) {
                val responses = response.responses
                processNotificationResponses(responses, tokens)
                log.info("[ANDROID] success {}", response.successCount > 0)
                return response.successCount > 0
            }
             false
        } catch (exception: FirebaseMessagingException) {
            log.info("[ANDROID] error catch")
            return false
        }
    }

    private fun processNotificationResponses(responses: List<SendResponse>, tokens: List<String>) {
        responses.forEachIndexed { index, sendResponse ->
            if (!sendResponse.isSuccessful) {
                val token = tokens[index]
                val exception = sendResponse.exception
                if (exception is FirebaseMessagingException) {
                    val errorCode = exception.errorCode

                    log.info("[ANDROID] error {}", exception)
                    if (errorCode.name == "REGISTRATION_TOKEN_NOT_REGISTERED") {

                        //TODO: add delete token / archive token function
                        // tokenRepository.deleteByToken(token)
                    }
                }
            }
        }
    }


    private fun sendDebugNotifications(tokens: List<String>, title: String, body: String) {
        val summary = "[" + shorten(title, 15) + "... | " + shorten(title, 15) + "...]"
        for (token in tokens) {
            log.debug("Sending fake DEBUG notification $summary to: ", token)
        }
    }


    private fun collectTokens(
        users: List<User>,
        androidTokens: MutableList<String>,
        appleTokens: MutableList<String>,
        debugTokens: MutableList<String>
    ) {
        for (user in users) {
            val devices: List<Device> = deviceRepository.findByUser(user)

            for (device in devices) {

                val token: String? = device.token

                token?.let {
                    when (device.platform) {
                        PlatformType.ANDROID -> androidTokens.add(token)
                        PlatformType.IOS -> appleTokens.add(token)
                        PlatformType.DEBUG -> debugTokens.add(token)
                        else -> log.debug("Unknown platform for notification device: ", device)
                    }
                }
            }

        }
    }


    private fun collectAllTokens(
        androidTokens: MutableList<String>,
        appleTokens: MutableList<String>,
         debugTokens: MutableList<String>) {


        val devices = deviceRepository.findAll()
        for (device in devices) {

            val token: String? = device.token

            token?.let {
                when (device.platform) {
                    PlatformType.ANDROID -> androidTokens.add(token)
                    PlatformType.IOS -> appleTokens.add(token)
                    PlatformType.DEBUG -> debugTokens.add(token)
                    else -> log.debug("Unknown platform for notification device: ", device)
                }
            }
        }

    }


    fun sendNotification(users: List<User>, notification: NotificationDTO): Boolean {
        val title = notification.title ?: throw IllegalArgumentException("Title is required for notification")
        val body = notification.body  ?: throw IllegalArgumentException("Body is required for notification")
        val route = notification.route ?: throw IllegalArgumentException("Route is required for notification")

        val androidTokens: MutableList<String> = mutableListOf()
        val  appleTokens: MutableList<String> = mutableListOf()
        val  debugTokens: MutableList<String> = mutableListOf()

        collectTokens(users, androidTokens, appleTokens,debugTokens)


        val androidTokensChunks = androidTokens.chunked(500)
        val appleTokensChunks = appleTokens.chunked(500)

        var androidSuccess = false
        var iOSSuccess = false

        for (chunk in androidTokensChunks) {
            androidSuccess = sendAndroidNotifications(androidTokens, title,  body, route)
        }

        for(chunk in appleTokensChunks) {
            iOSSuccess = sendAppleNotifications(appleTokens, title, body, route)
        }



        log.info("[ANDROID - success] androidSuccss {}", androidSuccess)

        sendDebugNotifications(debugTokens, title, body)

        return androidSuccess || iOSSuccess
    }


    fun sendNotificationToAll(notification: NotificationDTO) {
        val title = notification.title ?: throw IllegalArgumentException("Title is required for notification")
        val body = notification.body  ?: throw IllegalArgumentException("Body is required for notification")
        val route = notification.route ?: throw IllegalArgumentException("Route is required for notification")

        val androidTokens: MutableList<String> = mutableListOf()
        val  appleTokens: MutableList<String> = mutableListOf()
        val  debugTokens: MutableList<String> = mutableListOf()

        collectAllTokens(androidTokens, appleTokens,debugTokens)

        val androidTokensChunks = androidTokens.chunked(500)
        val appleTokensChunks = appleTokens.chunked(500)

        for (chunk in androidTokensChunks) {
            sendAndroidNotifications(androidTokens, title,  body, route)
        }

        for(chunk in appleTokensChunks) {
            sendAppleNotifications(appleTokens, title, body, route)
        }

        sendDebugNotifications(debugTokens, title, body)
    }

    fun scheduleNotification(contentGroup: QueryContentGroup?, latestEvaluation: QueryEvaluation, subject: Subject, channel: String = "high_importance_channel") {
        contentGroup?.let {

            val contentNotification = ContentNotification()
            contentNotification.contentGroup = contentGroup
            contentNotification.createdOn = ZonedDateTime.now()
            contentNotification.subject = subject
            contentNotification.channel = channel
            contentNotification.sent = false

            contentNotificationRepository.saveAndFlush(contentNotification)
        }
    }



    companion object {
        private val log = LoggerFactory.getLogger(NotificationService::class.java)
    }

}
