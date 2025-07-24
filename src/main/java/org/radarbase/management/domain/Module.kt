package org.radarbase.management.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import org.radarbase.management.domain.support.AbstractEntityListener
import java.io.Serializable
import javax.persistence.*


@Entity
@Table(name = "modules")
@EntityListeners(AbstractEntityListener::class)
class Module : AbstractEntity(), Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", initialValue = 1000, sequenceName = "hibernate_sequence")
    override var id: Long? = null

    @Column(name = "name")
    var name: String? = null

    @Column(name = "group_name")
    var groupName: String? = null

    @Column(name = "link")
    var link: String? = null

}
