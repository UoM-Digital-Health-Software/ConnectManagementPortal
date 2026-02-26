package org.radarbase.management.domain

import org.hibernate.annotations.Cascade
import org.hibernate.annotations.CascadeType
import org.radarbase.management.domain.enumeration.AppConfigType
import org.radarbase.management.domain.support.AbstractEntityListener
import java.time.Instant
import javax.persistence.*
import java.io.Serializable
import java.time.ZonedDateTime

@Entity
@Table(name = "cache_size_log")
@EntityListeners(AbstractEntityListener::class)
class CacheSizeLog: AbstractEntity(), Serializable{
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", initialValue = 1000, sequenceName = "hibernate_sequence")
    override var id: Long? = null


    @Column(name = "user_id")
    var userId: Long? = null

    @Column(name = "value")
    var value: Long? = null

    @Column(name = "created_on")
    var createdOn: ZonedDateTime? = null



}
