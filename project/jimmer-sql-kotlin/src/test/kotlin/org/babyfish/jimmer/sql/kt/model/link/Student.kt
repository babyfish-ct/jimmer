package org.babyfish.jimmer.sql.kt.model.link

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.ManyToManyView
import org.babyfish.jimmer.sql.OneToMany

@Entity
interface Student {

    @Id
    val id: Long

    val name: String

    @OneToMany(mappedBy = "student")
    val learningLinks: List<LearningLink>

    @ManyToManyView(prop = "learningLinks")
    val courses: List<Course>
}