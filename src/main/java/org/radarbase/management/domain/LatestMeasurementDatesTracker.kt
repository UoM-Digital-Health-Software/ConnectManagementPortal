package org.radarbase.management.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.radarbase.management.domain.support.AbstractEntityListener
import java.io.Serializable
import java.time.ZonedDateTime
import javax.persistence.*
import javax.validation.constraints.NotNull


@Entity
@Table(name = "latest_measurement_dates_tracker")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@EntityListeners(
    AbstractEntityListener::class
)
class LatestMeasurementDatesTracker(
    @Id @GeneratedValue(
    strategy = GenerationType.SEQUENCE,
    generator = "sequenceGenerator"
) @SequenceGenerator(
    name = "sequenceGenerator",
    initialValue = 1000,
    sequenceName = "hibernate_sequence"
) override var id: Long? = null) : AbstractEntity(), Serializable {

    @JvmField
    @Column(name = "summary_id")
    var summaryId: String? = null

    @JvmField
    @Column(name = "request_on")
    var requestedOn: ZonedDateTime? = null

    @Column(name = "generated")
    @NotNull
    var generated: Boolean = false

    @Column(name = "loaded")
    @NotNull
    var loaded: Boolean = false
}
