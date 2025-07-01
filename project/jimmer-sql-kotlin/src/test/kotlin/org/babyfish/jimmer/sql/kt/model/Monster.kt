package org.babyfish.jimmer.sql.kt.model

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.ManyToOne

@Entity
interface Monster {
    @Id
    val id: Int

    // Previously, using `base` would cause conflicts with local variables in the generated `Draft` file,
    // resulting in compilation failures.
    // Now, as long as it can generate the ` Draft ` file correctly and pass compilation, the problem is solved.
    @ManyToOne
    val base: Monster?
}