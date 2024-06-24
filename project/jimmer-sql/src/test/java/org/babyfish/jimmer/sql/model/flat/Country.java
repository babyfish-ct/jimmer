package org.babyfish.jimmer.sql.model.flat;

import org.babyfish.jimmer.sql.*;

import java.util.List;

@Entity
@Table(name = "FLAT_COUNTRY")
@DatabaseValidationIgnore
public interface Country {

    @Id
    long id();

    String countryName();

    @OneToMany(mappedBy = "country")
    List<Province> provinces();
}
