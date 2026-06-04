package org.babyfish.jimmer.sql.model.mapsid;

import org.babyfish.jimmer.sql.JoinColumn;
import org.babyfish.jimmer.sql.ManyToOne;
import org.babyfish.jimmer.sql.MappedSuperclass;
import org.babyfish.jimmer.sql.MapsId;

@MappedSuperclass
public interface MappedTenantDocumentBase {

    @ManyToOne
    @MapsId("tenantId")
    @JoinColumn(name = "TENANT_ID", referencedColumnName = "ID")
    Tenant tenant();
}
