package org.babyfish.jimmer.sql.kt.model.hr

import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongConverter
import org.babyfish.jimmer.jackson.LongListConverter
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.IdView
import org.babyfish.jimmer.sql.OneToMany

@Entity
interface Department {

    @Id
    @JsonConverter(LongConverter::class)
    val id: Long

    val name: String

    @OneToMany(mappedBy = "department")
    val employees: List<Employee>

    @IdView("employees")
    @JsonConverter(LongListConverter::class)
    val employeeIds: List<Long>
}