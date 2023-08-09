package org.babyfish.jimmer.sql.model.flat;

import org.babyfish.jimmer.sql.*;

@Entity
@Table(name = "FLAT_PROVINCE")
@DatabaseValidationIgnore
public interface Province {

    @Id
    long id();

    String provinceName();

    @ManyToOne
    Country country();
}
