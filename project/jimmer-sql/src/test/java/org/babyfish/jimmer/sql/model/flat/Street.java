package org.babyfish.jimmer.sql.model.flat;

import org.babyfish.jimmer.sql.*;

@Entity
@Table(name = "FLAT_STREET")
@DatabaseValidationIgnore
public interface Street {

    @Id
    long id();

    String streetName();

    @ManyToOne
    City city();
}
