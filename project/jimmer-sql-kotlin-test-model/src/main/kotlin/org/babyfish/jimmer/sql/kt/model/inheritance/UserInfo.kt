package org.babyfish.jimmer.sql.kt.model.inheritance

import org.babyfish.jimmer.sql.MappedSuperclass

@MappedSuperclass
interface UserInfo {

    val name: String
}