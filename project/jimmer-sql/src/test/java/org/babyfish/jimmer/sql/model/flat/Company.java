package org.babyfish.jimmer.sql.model.flat;

import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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
