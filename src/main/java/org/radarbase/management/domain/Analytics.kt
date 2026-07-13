package org.radarbase.management.domain

import org.radarbase.management.domain.enumeration.AuditEvent
import org.radarbase.management.domain.support.AbstractEntityListener
import java.io.Serializable
import java.time.Instant
import java.time.LocalDate
import javax.persistence.*

@Entity
@Table(name = "analytics")
@EntityListeners(AbstractEntityListener::class)
class Analytics : AbstractEntity(), Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", initialValue = 1000, sequenceName = "hibernate_sequence")
    override var id: Long? = null

    @Column(name = "date")
    var date: Instant? = Instant.now()


    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "participant_id")
    var participant: Subject? = null


    @Enumerated(EnumType.STRING)
    var action: AuditEvent? = null

    @Column(name = "page_url")
    var pageUrl: String? = null

    @Column(name = "category")
    var category: String? = null
}
