package org.babyfish.jimmer.sql.kt.model.logic.instant

import org.babyfish.jimmer.sql.DatabaseValidationIgnore
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.LogicalDeleted
import org.babyfish.jimmer.sql.Table
import java.time.Instant

/**
 * Kotlin equivalent of Java F.java entity for testing Instant type support
 * with @LogicalDeleted("null") annotation in KSP processing
 */
@Entity
@DatabaseValidationIgnore
@Table(name = "JIMMER_TEST_DB.F_KT.TABLE_F_KT")
interface FKt {

    @Id
    val id: Long

    @LogicalDeleted("null")
    val deletedTime: Instant?
}
