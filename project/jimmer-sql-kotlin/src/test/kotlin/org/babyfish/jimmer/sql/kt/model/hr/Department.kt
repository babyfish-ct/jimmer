package org.babyfish.jimmer.sql.kt.model.hr

import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongConverter
import org.babyfish.jimmer.jackson.LongListConverter
import org.babyfish.jimmer.sql.*
import java.time.LocalDateTime

@Entity
interface Department {

    @Id
    @JsonConverter(LongConverter::class)
    val id: Long

    val name: String

    @LogicalDeleted("now")
    val deletedTime: LocalDateTime?

    @OneToMany(mappedBy = "department")
    val employees: List<Employee>

    @IdView("employees")
    @JsonConverter(LongListConverter::class)
    val employeeIds: List<Long>
}