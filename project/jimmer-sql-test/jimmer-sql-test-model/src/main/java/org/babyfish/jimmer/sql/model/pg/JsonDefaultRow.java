package org.babyfish.jimmer.sql.model.pg;

import org.babyfish.jimmer.sql.DatabaseValidationIgnore;
import org.babyfish.jimmer.sql.Default;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Serialized;

import java.util.List;

@DatabaseValidationIgnore
@Entity
public interface JsonDefaultRow {

    @Id
    long id();

    String name();

    @Serialized
    @Default("[\"item\"]")
    List<String> tags();
}
