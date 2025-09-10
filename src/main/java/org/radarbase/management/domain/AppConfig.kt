package org.radarbase.management.domain


import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.envers.Audited
import org.radarbase.management.domain.enumeration.AppConfigType
import org.radarbase.management.domain.support.AbstractEntityListener
import java.time.Instant
import javax.persistence.*

@Entity
@Table(name = "app_config")
@EntityListeners(AbstractEntityListener::class)
 class AppConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    val site: String? = null

    @Column(name = "user_id")
    val userId: Long? = null

    val key: String? = null
    val value: String? = null

    @Enumerated(EnumType.STRING)
    val type: AppConfigType? = null

    @Column(name = "rollout_pct")
    val rolloutPct: Int? = null

    @Column(name = "rollout_version")
    val rolloutVersion: String? = null

    @Column(name = "conditional")
    val conditional: String? = null

    @Column(name = "created_at")
    val createdAt: Instant? = null

 }
