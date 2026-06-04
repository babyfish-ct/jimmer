package org.babyfish.jimmer.sql.model.mapsid;

import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.MappedSuperclass;
import org.babyfish.jimmer.sql.PropOverride;

@MappedSuperclass
public interface MappedTenantDocumentIdBase {

    @Id
    @PropOverride(prop = "tenantId", columnName = "TENANT_ID")
    @PropOverride(prop = "documentId", columnName = "DOCUMENT_ID")
    TenantDocumentId id();
}
