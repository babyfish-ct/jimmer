package org.babyfish.jimmer.sql.kt.model.provider

import org.babyfish.jimmer.Scalar
import org.babyfish.jimmer.sql.Column
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.GenerationType
import org.babyfish.jimmer.sql.Id
import java.util.EnumSet

@Entity
interface Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    val name: String

    @Scalar
    @Column(name = "tags_mask")
    val tags: EnumSet<Tag>
}