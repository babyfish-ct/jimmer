package org.babyfish.jimmer.sql.model.base;

import org.babyfish.jimmer.sql.MappedSuperclass;

@MappedSuperclass
public interface DocumentedBase {

    /**
     * The user who created the object
     */
    String getCreatedBy();
}
