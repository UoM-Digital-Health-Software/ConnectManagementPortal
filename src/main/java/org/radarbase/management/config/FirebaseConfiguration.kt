package org.radarbase.management.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.IOException

@Configuration
class FirebaseConfiguration {

    private val FIREBASE_APP_NAME = "actinow-64cf2"
    private val FIREBASE_CREDENTIALS_FILE = "notif_config/actinow-firebase-adminsdk.json"

    @Bean
    @Throws(IOException::class)
    fun firebaseMessaging(): FirebaseMessaging {
        val firebaseApp = try {
            FirebaseApp.getInstance(FIREBASE_APP_NAME)
        } catch (e: IllegalStateException) {
            val credentialsStream = javaClass.classLoader.getResourceAsStream(FIREBASE_CREDENTIALS_FILE)
                ?: throw IOException("Firebase credentials file not found in resources: $FIREBASE_CREDENTIALS_FILE")

            val googleCredentials = GoogleCredentials.fromStream(credentialsStream)
            val firebaseOptions = FirebaseOptions.builder()
                .setCredentials(googleCredentials)
                .build()

            FirebaseApp.initializeApp(firebaseOptions, FIREBASE_APP_NAME)
        }

        return FirebaseMessaging.getInstance(firebaseApp)
    }
}
