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
    var id: Long = 0

    var site: String? = null

    @Column(name = "user_id")
    var userId: Long? = null

    var key: String? = null
    var value: String? = null

    @Enumerated(EnumType.STRING)
    var type: AppConfigType? = null

    @Column(name = "rollout_pct")
    var rolloutPct: Int? = null

    @Column(name = "rollout_version")
    var rolloutVersion: String? = null

    @Column(name = "conditional")
    var conditional: String? = null

    @Column(name = "created_at")
    var createdAt: Instant? = null

 }
