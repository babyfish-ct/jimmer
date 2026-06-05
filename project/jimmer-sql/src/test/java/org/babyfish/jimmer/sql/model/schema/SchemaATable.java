package org.babyfish.jimmer.sql.model.schema;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Table;

@Entity
@Table(name = "MY_TABLE", schema = "SCHEMA_A")
public interface SchemaATable {

    @Id
    long id();
}
