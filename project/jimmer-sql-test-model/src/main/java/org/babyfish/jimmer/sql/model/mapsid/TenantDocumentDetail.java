package org.babyfish.jimmer.sql.model.mapsid;

import org.babyfish.jimmer.sql.*;

@Entity
public interface TenantDocumentDetail {

    @Id
    @PropOverride(prop = "tenantId", columnName = "TENANT_ID")
    @PropOverride(prop = "documentId", columnName = "DOCUMENT_ID")
    TenantDocumentId id();

    String description();

    @OneToOne
    @MapsId
    @JoinColumn(name = "TENANT_ID", referencedColumnName = "TENANT_ID")
    @JoinColumn(name = "DOCUMENT_ID", referencedColumnName = "DOCUMENT_ID")
    TenantDocument document();
}
