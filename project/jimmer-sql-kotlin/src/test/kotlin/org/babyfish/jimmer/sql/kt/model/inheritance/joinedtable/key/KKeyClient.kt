package org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable.key

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.GenerationType
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Inheritance
import org.babyfish.jimmer.sql.InheritanceType
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.KeyUniqueConstraint
import org.babyfish.jimmer.sql.Table

@Entity
@Table(name = "JOINED_KEY_CLIENT")
@Inheritance(strategy = InheritanceType.JOINED)
@KeyUniqueConstraint
interface KKeyClient : KKeyClientBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    @Key
    val code: String

    val name: String
}
