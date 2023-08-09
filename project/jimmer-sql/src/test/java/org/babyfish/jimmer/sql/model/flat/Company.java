package org.babyfish.jimmer.sql.model.flat;

import org.babyfish.jimmer.sql.*;

@Entity
@Table(name = "FLAT_COMPANY")
@DatabaseValidationIgnore
public interface Company {

    @Id
    long id();

    String companyName();

    @ManyToOne
    Street street();
}
