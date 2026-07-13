package org.babyfish.jimmer.sql.kt.model.inheritance.joinedtable.inverse

import org.babyfish.jimmer.sql.DiscriminatorValue
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.IdView
import org.babyfish.jimmer.sql.JoinColumn
import org.babyfish.jimmer.sql.OneToOne
import org.babyfish.jimmer.sql.Table

@Entity
@Table(name = "JOINED_PASSPORT")
@DiscriminatorValue("PASSPORT")
interface KPassport : KDocument {

    @OneToOne
    @JoinColumn(name = "CITIZEN_ID")
    val citizen: KCitizen?

    @IdView
    val citizenId: Long?
}
