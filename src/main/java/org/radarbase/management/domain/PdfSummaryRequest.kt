package org.radarbase.management.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.radarbase.management.domain.support.AbstractEntityListener
import java.io.Serializable
import java.time.ZonedDateTime
import java.util.*
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Table(name = "pdf_summary_request")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@EntityListeners(
    AbstractEntityListener::class
)
class PdfSummaryRequest(
    @Id @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "sequenceGenerator"
    ) @SequenceGenerator(
        name = "sequenceGenerator",
        initialValue = 1000,
        sequenceName = "hibernate_sequence"
    ) override var id: Long? = null
) : AbstractEntity(), Serializable {


    @JvmField
    @OneToOne
    @JoinColumn(unique = true, name = "subject_id")
    var subject: Subject? = null

    @JvmField
    @Column(name = "summary_id")
    var summaryId: String? = null


    @JvmField
    @Column(name = "request_on")
    var requestedOn: ZonedDateTime? = null

    @JvmField
    @OneToOne
    @JoinColumn(unique = true, name = "requested_by")
    var requestedBy: User? = null

    @JvmField
    @Column(name = "summary_created_on")
    var summaryCreatedOn: ZonedDateTime? = null


    @Column(name = "email_sent")
    @NotNull
    var emailSent: Boolean = false


    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val subject = other as Subject
        return if (subject.id == null || id == null) {
            false
        } else id == subject.id
    }

    override fun hashCode(): Int {
        return Objects.hashCode(id)
    }

    override fun toString(): String {
        return ("Subject{"
                + "id=" + id
                + ", subbject='" + subject + '\''
                + ", summaryId='" + summaryId + '\''
                + ", requestedOn=" + requestedOn
                + ", requestedBy=" + requestedBy
                + ", summaryCreatedOn=" + summaryCreatedOn
                + ", emailSent=" + emailSent
                + "}")
    }


}
