package org.radarbase.management.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.radarbase.management.domain.support.AbstractEntityListener
import java.io.Serializable
import java.time.Instant
import javax.persistence.*


@Entity
@Table(name = "connect_data_log_aws")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@EntityListeners(
    AbstractEntityListener::class
)
class ConnectDataLogAWS(
    @Id @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "sequenceGenerator"
    ) @SequenceGenerator(
        name = "sequenceGenerator",
        initialValue = 1000,
        sequenceName = "hibernate_sequence"
    ) override var id: Long? = null) : AbstractEntity(), Serializable

{
    @Column(name = "user_id")
    var userId: String? = null

    @Column(name = "project_id")
    var projectId: String? = null

    @Column(name = "data_grouping_type")
    var dataGroupingType: String? = null

    @Column(name = "time")
    var time: Instant? = null

    @Column(name = "created_at")
    var createdAt: Instant? = null


}
