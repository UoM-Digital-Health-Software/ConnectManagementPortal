package org.radarbase.management.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.annotations.Cascade
import org.hibernate.annotations.CascadeType
import org.hibernate.envers.Audited
import org.radarbase.management.domain.enumeration.ContentGroupStatus
import org.radarbase.management.domain.support.AbstractEntityListener
import org.radarbase.management.service.catalog.ContentItem
import java.io.Serializable
import java.time.ZonedDateTime
import java.util.HashSet
import javax.persistence.*


@Entity
@Audited
@Table(name = "query_content_group")
@EntityListeners(AbstractEntityListener::class)
class QueryContentGroup : AbstractEntity(), Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", initialValue = 1000, sequenceName = "hibernate_sequence")
    override var id: Long? = null

    @Column(name = "content_group_name", nullable = false)
    var contentGroupName: String? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "query_group_id", nullable = false)
    var queryGroup: QueryGroup? = null

    @Column(name = "created_date")
    var createdDate: ZonedDateTime? = null

    @Column(name = "updated_date")
    var updatedDate: ZonedDateTime? = null

    @Column(name = "is_archived")
    var isArchived: Boolean? = false


    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: ContentGroupStatus = ContentGroupStatus.INACTIVE

    @JvmField
    @OneToMany(mappedBy = "queryContentGroup", orphanRemoval = true, fetch = FetchType.EAGER)
    @Cascade(
        CascadeType.ALL
    )
    var contentItems: MutableSet<QueryContent> = HashSet()


    override fun toString(): String {
        return ("QueryContentGroup{"
                + "ContentGroupName='" + contentGroupName + '\''
                + ", queryGroup='" + queryGroup?.name + '\''
                + ", createdDate=" + createdDate
                + ", updatedDate=" + updatedDate
                + "}")
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}
