package org.babyfish.jimmer.sql.model.type;

import org.babyfish.jimmer.sql.DatabaseValidationIgnore;
import org.babyfish.jimmer.sql.Entity;

@DatabaseValidationIgnore
@Entity
public interface ClassNode extends Annotated {

    String typeName();
}
