package org.babyfish.jimmer.sql.kt.model.link

import org.babyfish.jimmer.sql.*

@Entity
interface LearningLink {

    @Id
    val id: Long

    @ManyToOne
    @OnDissociate(DissociateAction.DELETE)
    val student: Student

    @ManyToOne
    @OnDissociate(DissociateAction.DELETE)
    val course: Course

    val score: Int?
}