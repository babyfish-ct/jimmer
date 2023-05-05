package org.babyfish.jimmer.sql.model.type;

import org.babyfish.jimmer.sql.DatabaseValidationIgnore;
import org.babyfish.jimmer.sql.Entity;

@Entity
@DatabaseValidationIgnore
public interface InterfaceNode extends Annotated {

    String typeName();
}
