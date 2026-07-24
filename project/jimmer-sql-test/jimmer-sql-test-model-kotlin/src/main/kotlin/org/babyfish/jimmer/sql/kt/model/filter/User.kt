package org.babyfish.jimmer.sql.kt.model.filter

import org.babyfish.jimmer.sql.*
import testpkg.annotations.Module
import testpkg.annotations.Serializable
import java.time.LocalDateTime

@Entity
@Table(name = "file_user")
@Module("a", "b", "c")
@Serializable(with = User::class)
interface User {
    @Id
    val id: Long

    @Serializable(with = String::class)
    @Key
    val name: String

    @ManyToMany(mappedBy = "users")
    val files: List<File>

    @LogicalDeleted(value = "now")
    val deletedTime: LocalDateTime?
}

