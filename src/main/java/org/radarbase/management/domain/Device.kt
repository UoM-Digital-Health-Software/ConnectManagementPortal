package org.radarbase.management.domain

import org.radarbase.management.domain.enumeration.PlatformType
import java.io.Serializable
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Table(name = "device", uniqueConstraints = [UniqueConstraint(columnNames = ["token"])])
class Device : Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @OneToOne(cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    var user: @NotNull User? = null

    @Enumerated(EnumType.STRING)
    var platform: @NotNull PlatformType? = null

    @Column(nullable = false, unique = true)
    var token: @NotNull String? = null



    override fun toString(): String {
        return "ParticipantDevice [" +
                "id=" + id +
                ", platform=" + platform +
                ", token=" + token +
                "]"
    }
}
