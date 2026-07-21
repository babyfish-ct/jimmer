package org.babyfish.jimmer.sql.model.inheritance.joinedtable.inverse;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.GeneratedValue;
import org.babyfish.jimmer.sql.GenerationType;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.OneToOne;
import org.babyfish.jimmer.sql.Table;
import org.jetbrains.annotations.Nullable;

@Entity
@Table(name = "JOINED_CITIZEN")
public interface Citizen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    String name();

    @Nullable
    @OneToOne(mappedBy = "citizen")
    Passport passport();
}
