package org.babyfish.jimmer.sql.model.mapsid;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Table;

@Entity
@Table(name = "MAPPED_TENANT_DOCUMENT")
public interface MappedTenantDocument extends MappedTenantDocumentIdBase, MappedTenantDocumentBase {

    String name();
}
