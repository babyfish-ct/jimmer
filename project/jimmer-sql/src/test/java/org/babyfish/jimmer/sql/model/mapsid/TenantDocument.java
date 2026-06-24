package org.babyfish.jimmer.sql.model.mapsid;

import org.babyfish.jimmer.sql.*;

@Entity
public interface TenantDocument {

    @Id
    @PropOverride(prop = "tenantId", columnName = "TENANT_ID")
    @PropOverride(prop = "documentId", columnName = "DOCUMENT_ID")
    TenantDocumentId id();

    String name();

    @ManyToOne
    @MapsId("tenantId")
    @JoinColumn(name = "TENANT_ID", referencedColumnName = "ID")
    Tenant tenant();
}
