package org.babyfish.jimmer.sql.model.mapsid;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.OneToMany;

import java.util.List;

@Entity
public interface Tenant {

    @Id
    long id();

    String name();

    @OneToMany(mappedBy = "tenant")
    List<TenantDocument> documents();
}
