package org.babyfish.jimmer.sql.model.doc;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.model.base.DocumentedBase;

@Entity
public interface DocumentedEntity extends DocumentedBase {

    @Id
    long getId();
}
