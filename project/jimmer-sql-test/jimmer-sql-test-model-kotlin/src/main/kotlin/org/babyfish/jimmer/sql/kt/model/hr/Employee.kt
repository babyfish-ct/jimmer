package org.babyfish.jimmer.sql.kt.model.hr

import org.babyfish.jimmer.json.JsonFormat
import org.babyfish.jimmer.json.converter.JsonConverter
import org.babyfish.jimmer.json.converter.LongToStringConverter
import org.babyfish.jimmer.sql.*
import testpkg.annotations.Job
import testpkg.annotations.Priority
import testpkg.annotations.Task
import java.util.*

@Entity
interface Employee {

    @Id
    @JsonConverter(LongToStringConverter::class)
    val id: Long

    @Column(name = "NAME")
    @get:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @get:Job([
        Task("a", priority = Priority.LOW),
        Task("b", priority = Priority.NORMAL),
        Task("c", priority = Priority.HIGH)
    ])
    val employeeName: String

    @ManyToOne
    @OnDissociate(DissociateAction.DELETE)
    val department: Department?

    @LogicalDeleted
    val deletedUUID: UUID?
}