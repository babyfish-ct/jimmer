package org.babyfish.jimmer.example.kt.graphql.entities

import org.babyfish.jimmer.example.kt.graphql.entities.common.BaseEntity
import org.babyfish.jimmer.sql.*

@Entity
interface Chapter : BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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