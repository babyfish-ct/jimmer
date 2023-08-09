package org.babyfish.jimmer.sql.model.flat;

import org.babyfish.jimmer.sql.*;

@Entity
@Table(name = "FLAT_CITY")
@DatabaseValidationIgnore
public interface City {

    @Id
    long id();

    String cityName();

    @ManyToOne
    Province province();
}
