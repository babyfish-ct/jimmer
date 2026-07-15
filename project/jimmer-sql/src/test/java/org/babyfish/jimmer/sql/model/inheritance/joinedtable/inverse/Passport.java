package org.babyfish.jimmer.sql.model.inheritance.joinedtable.inverse;

import org.babyfish.jimmer.sql.DiscriminatorValue;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.IdView;
import org.babyfish.jimmer.sql.JoinColumn;
import org.babyfish.jimmer.sql.OneToOne;
import org.babyfish.jimmer.sql.Table;
import org.jetbrains.annotations.Nullable;

@Entity
@Table(name = "JOINED_PASSPORT")
@DiscriminatorValue("PASSPORT")
public interface Passport extends Document {

    @Nullable
    @OneToOne
    @JoinColumn(name = "CITIZEN_ID")
    Citizen citizen();

    @Nullable
    @IdView
    Long citizenId();
}
