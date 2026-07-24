package org.babyfish.jimmer.sql.kt.model.logic.instant

import org.babyfish.jimmer.sql.DatabaseValidationIgnore
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.LogicalDeleted
import org.babyfish.jimmer.sql.Table
import java.time.Instant

/**
 * Kotlin equivalent of Java E.java entity for testing Instant type support
 * with @LogicalDeleted("now") annotation in KSP processing
 */
@Entity
@DatabaseValidationIgnore
@Table(name = "JIMMER_TEST_DB.E_KT.TABLE_E_KT")
interface EKt {

    @Id
    val id: Long

    @LogicalDeleted("now")
    val deletedTime: Instant?
}
