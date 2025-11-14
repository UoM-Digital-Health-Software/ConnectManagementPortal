package org.radarbase.management.domain

import org.hibernate.envers.Audited
import org.radarbase.management.domain.support.AbstractEntityListener
import java.io.Serializable
import java.time.Instant
import java.time.ZonedDateTime
import javax.persistence.*


@Entity
@Table(name = "content_notification")
@EntityListeners(AbstractEntityListener::class)
class ContentNotification : AbstractEntity(), Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(
        name = "sequenceGenerator",
        initialValue = 1000,
        sequenceName = "hibernate_sequence"
    )
    override var id: Long? = null





    @Column(name = "channel", nullable = true )
    var channel: String? = null

    @Column(name = "sent", nullable = false)
    var sent: Boolean = false

    @Column(name = "created_on", nullable = false)
    var createdOn: ZonedDateTime = ZonedDateTime.now()

    @Column(name = "sent_on")
    var sentOn: ZonedDateTime? = null

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subject_id", nullable = false)
    var subject: Subject? = null

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "content_group_id")
    var contentGroup: QueryContentGroup? = null

    override fun toString(): String {
        return "NotificationContent{" +
                "id=$id, " +

                "channel=$channel, " +
                "sent=$sent, " +
                "createdOn=$createdOn, " +
                "sentOn=$sentOn" +
                "}"
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}
