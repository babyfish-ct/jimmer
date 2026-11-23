package org.babyfish.jimmer.nojackson

import org.babyfish.jimmer.client.meta.Api
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id

@Entity
interface KNote {
    @Id val id: Long
    val label: String?
}

@Api
interface KNoteApi {
    @Api fun note(id: Long): KNote
}
