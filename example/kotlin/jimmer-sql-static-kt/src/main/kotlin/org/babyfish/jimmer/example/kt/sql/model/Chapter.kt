package org.babyfish.jimmer.example.kt.sql.model

import org.babyfish.jimmer.example.kt.sql.model.common.BaseEntity
import org.babyfish.jimmer.pojo.AutoScalarStrategy
import org.babyfish.jimmer.pojo.StaticType
import org.babyfish.jimmer.sql.*

@Entity
interface Chapter : BaseEntity {

    @Id
    val id: Long

    @Key
    @ManyToOne(inputNotNull = true)
    @OnDissociate(DissociateAction.DELETE)
    val book: Book?

    @Key
    @Column(name = "chapter_no")
    val index: Int

    val title: String
}