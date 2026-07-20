package org.babyfish.jimmer.sql.model.mapsid;

import org.babyfish.jimmer.sql.Embeddable;

@Embeddable
public interface TenantDocumentId {

    long tenantId();

    long documentId();
}
