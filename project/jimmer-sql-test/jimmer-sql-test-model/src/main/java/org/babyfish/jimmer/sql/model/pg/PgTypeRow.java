package org.babyfish.jimmer.sql.model.pg;

import org.babyfish.jimmer.sql.*;

@Entity
@DatabaseValidationIgnore
public interface PgTypeRow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    String macAddress();
}
