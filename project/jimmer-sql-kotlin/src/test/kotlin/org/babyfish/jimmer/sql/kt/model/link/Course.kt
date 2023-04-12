package org.babyfish.jimmer.sql.kt.model.link

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.ManyToManyView
import org.babyfish.jimmer.sql.OneToMany

@Entity
interface Course {

    @Id
    val id: Long

    val name: String

    val academicCredit: Int

    @OneToMany(mappedBy = "course")
    val learningLinks: List<LearningLink>

    @ManyToManyView(prop = "learningLinks", deeperProp = "student")
    val students: List<Student>
}