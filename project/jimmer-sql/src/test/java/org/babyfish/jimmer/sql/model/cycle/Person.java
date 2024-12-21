package org.babyfish.jimmer.sql.model.cycle;

import org.babyfish.jimmer.sql.*;
import org.jetbrains.annotations.Nullable;

@Entity
public interface Person {

    @Id
    long id();

    String name();

    @Nullable
    @ManyToOne
    @OnDissociate(DissociateAction.DELETE)
    Person friend();
}
