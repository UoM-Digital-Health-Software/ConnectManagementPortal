package org.radarbase.management.domain

import org.radarbase.management.domain.enumeration.AuditEvent
import org.radarbase.management.domain.support.AbstractEntityListener
import java.io.Serializable
import java.time.LocalDate
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.Lob
import javax.persistence.ManyToOne
import javax.persistence.SequenceGenerator
import javax.persistence.Table


@Entity
@Table(name = "analytics_parameters")
@EntityListeners(AbstractEntityListener::class)
class AnalyticsParameter : AbstractEntity(), Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", initialValue = 1000, sequenceName = "hibernate_sequence")
    override var id: Long? = null

    @Column(name = "parameter_name")
    var parameterName: String? = null


    @Column(name = "parameter_value")
    var parameterValue: String? = null

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "analytics_id")
    var analytics: Analytics? = null


}
