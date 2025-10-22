package org.radarbase.management.service

import com.google.firebase.messaging.*
import com.hazelcast.internal.util.StringUtil.shorten
import org.radarbase.management.domain.Device
import org.radarbase.management.domain.User
import org.radarbase.management.domain.enumeration.PlatformType
import org.radarbase.management.repository.DeviceRepository
import org.radarbase.management.repository.MetaTokenRepository
import org.radarbase.management.repository.UserRepository
import org.radarbase.management.service.dto.DeviceDTO
import org.radarbase.management.service.dto.NotificationDTO
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*


@Service
@Transactional
class NotificationService(
    private val userRepository: UserRepository,
    private val deviceRepository: DeviceRepository,
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


    private fun sendAppleNotifications(tokens: List<String>, title: String, body: String, route: String) {
        var title: String? = title
        var body: String? = body
        if (tokens.isEmpty()) {
            return
        }
        title = shorten(title, MAXIMUM_TITLE_LENGTH)
        body = shorten(body, APPLE_MAXIMUM_BODY_LENGTH)
        val message = MulticastMessage.builder()
            .setNotification(
                Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build()
            )
            .putData("route", route)
            .addAllTokens(tokens) // up to 500 max
            .build()
        try {
            val response: BatchResponse? = firebaseMessaging?.sendEachForMulticast(message)

        } catch (exception: FirebaseMessagingException) {

        }
    }

    private fun sendAndroidNotifications(tokens: List<String>, title: String, body: String, route: String) {
        var title: String? = title
        var body: String? = body
        if (tokens.isEmpty()) {
            return
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


        try {
            val response = firebaseMessaging?.sendEachForMulticast(message)
        } catch (exception: FirebaseMessagingException) {
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


    fun sendNotification(users: List<User>, notification: NotificationDTO) {
        val title = notification.title ?: throw IllegalArgumentException("Title is required for notification")
        val body = notification.body  ?: throw IllegalArgumentException("Body is required for notification")
        val route = notification.route ?: throw IllegalArgumentException("Route is required for notification")

        val androidTokens: MutableList<String> = mutableListOf()
        val  appleTokens: MutableList<String> = mutableListOf()
        val  debugTokens: MutableList<String> = mutableListOf()

        collectTokens(users, androidTokens, appleTokens,debugTokens)


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


    companion object {
        private val log = LoggerFactory.getLogger(NotificationService::class.java)
    }

}
