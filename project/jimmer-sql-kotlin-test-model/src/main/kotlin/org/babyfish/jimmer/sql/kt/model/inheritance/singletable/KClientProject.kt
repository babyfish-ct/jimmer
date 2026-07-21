package org.babyfish.jimmer.sql.kt.model.inheritance.singletable

import org.babyfish.jimmer.sql.*

@Entity
@Table(name = "SINGLE_CLIENT_PROJECT")
interface KClientProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    val name: String

    @ManyToOne
    val client: KClient?

    @ManyToMany
    @JoinTable(
        name = "SINGLE_CLIENT_PROJECT_PARTICIPANT_MAPPING",
        joinColumnName = "PROJECT_ID",
        inverseJoinColumnName = "CLIENT_ID",
        readonly = true,
        cascadeDeletedByTarget = true
    )
    val participants: List<KClient>
}
