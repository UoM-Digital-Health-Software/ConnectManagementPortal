package org.radarbase.management.domain

import org.hibernate.envers.Audited
import org.radarbase.management.domain.enumeration.CbtRouteSelectionMode
import org.radarbase.management.domain.support.AbstractEntityListener
import java.io.Serializable
import java.time.ZonedDateTime
import javax.persistence.*

@Entity
@Table(
    name = "query_participant_cbt_assignment",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_qpca_subject_participant_content_query_content",
            columnNames = ["subject_id", "query_participant_content_id", "query_content_id"]
        )
    ]
)
@EntityListeners(
    AbstractEntityListener::class
)
class QueryParticipantCbtAssignment : AbstractEntity(), Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(
        name = "sequenceGenerator",
        initialValue = 1000,
        sequenceName = "hibernate_sequence"
    )
    override var id: Long? = null

    @ManyToOne
    @JoinColumn(name = "subject_id", nullable = false)
    var subject: Subject? = null

    @ManyToOne
    @JoinColumn(name = "query_participant_content_id", nullable = false)
    var queryParticipantContent: QueryParticipantContent? = null

    @ManyToOne
    @JoinColumn(name = "query_content_id", nullable = false)
    var queryContent: QueryContent? = null

    @Column(name = "cbt_type", nullable = false)
    var cbtType: String? = null

    @Column(name = "cbt_version", nullable = false)
    var cbtVersion: String? = null

    @Column(name = "assigned_cbt_route", nullable = false)
    var assignedCbtRoute: String? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "selection_mode", nullable = false)
    var selectionMode: CbtRouteSelectionMode? = null

    @Column(name = "created_date", nullable = false)
    var createdDate: ZonedDateTime? = null

    @Column(name = "is_archived", nullable = false)
    var isArchived: Boolean = false

    override fun toString(): String {
        return ("QueryParticipantCbtAssignment{"
                + "id=" + id
                + ", cbtType='" + cbtType + '\''
                + ", cbtVersion='" + cbtVersion + '\''
                + ", assignedCbtRoute='" + assignedCbtRoute + '\''
                + ", selectionMode=" + selectionMode
                + ", createdDate=" + createdDate
                + ", isArchived=" + isArchived
                + "}")
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}
