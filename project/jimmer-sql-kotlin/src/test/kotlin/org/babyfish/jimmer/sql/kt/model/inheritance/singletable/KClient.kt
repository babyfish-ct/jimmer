package org.babyfish.jimmer.sql.kt.model.inheritance.singletable

import org.babyfish.jimmer.sql.Column
import org.babyfish.jimmer.sql.Discriminator
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.GenerationType
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Inheritance
import org.babyfish.jimmer.sql.InheritanceType
import org.babyfish.jimmer.sql.Table

@Entity
@Table(name = "CLIENT")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
interface KClient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    @Discriminator
    @Column(name = "CLIENT_TYPE")
    val type: String

    val name: String
}
