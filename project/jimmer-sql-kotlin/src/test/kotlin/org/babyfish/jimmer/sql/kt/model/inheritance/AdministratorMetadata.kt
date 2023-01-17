package org.babyfish.jimmer.sql.kt.model.inheritance

import org.babyfish.jimmer.pojo.StaticType
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.GenerationType
import org.babyfish.jimmer.sql.Id

@Entity
@StaticType(alias = "default", topLevelName = "AdminMetadataInput")
@StaticType(alias = "composite", topLevelName = "CompositeAdminMetadataInput")
interface AdministratorMetadata : AdministratorMetadataBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long
}