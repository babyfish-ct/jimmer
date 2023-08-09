package org.babyfish.jimmer.sql.model.flat;

import org.babyfish.jimmer.sql.DatabaseValidationIgnore;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Table;

@Entity
@Table(name = "FLAT_COUNTRY")
@DatabaseValidationIgnore
public interface Country {

    @Id
    long id();

    String countryName();
}
