package org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable.inverse

import org.babyfish.jimmer.sql.*

@Entity
@Table(name = "JOINED_DOCUMENT")
@Inheritance(strategy = InheritanceType.JOINED)
interface KDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    @Discriminator
    @Column(name = "DOCUMENT_TYPE")
    val type: String

    val name: String
}
