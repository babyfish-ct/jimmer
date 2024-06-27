package org.babyfish.jimmer.sql.model.flat;

import org.babyfish.jimmer.sql.*;

@Entity
public interface Company {

    @Id
    long id();

    String companyName();

    @ManyToOne
    @OnDissociate(DissociateAction.SET_NULL)
    Street street();
}
