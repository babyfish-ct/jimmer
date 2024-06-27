package org.babyfish.jimmer.sql.model.flat;

import org.babyfish.jimmer.sql.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Entity
public interface Company {

    @Id
    long id();

    String companyName();

    @Nullable
    @ManyToOne
    @OnDissociate(DissociateAction.SET_NULL)
    Street street();
}
