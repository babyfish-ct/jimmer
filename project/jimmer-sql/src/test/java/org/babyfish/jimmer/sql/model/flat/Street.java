package org.babyfish.jimmer.sql.model.flat;

import org.babyfish.jimmer.sql.*;

@Entity
public interface Street {

    @Id
    long id();

    String streetName();

    @ManyToOne
    @OnDissociate(DissociateAction.DELETE)
    City city();
}
