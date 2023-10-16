package org.babyfish.jimmer.sql.kt.model.filter

import org.babyfish.jimmer.sql.*
import java.time.LocalDateTime

@Entity
@Table(name = "file_user")
interface User {
    @Id
    val id: Long

    @Key
    val name: String

    @ManyToMany(mappedBy = "users")
    val files: List<File>

    @LogicalDeleted(value = "now")
    val deletedTime: LocalDateTime?
}

