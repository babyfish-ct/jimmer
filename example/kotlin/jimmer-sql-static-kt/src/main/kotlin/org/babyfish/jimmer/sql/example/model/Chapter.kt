package org.babyfish.jimmer.sql.example.model

import org.babyfish.jimmer.sql.*
import org.babyfish.jimmer.sql.example.model.common.BaseEntity

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