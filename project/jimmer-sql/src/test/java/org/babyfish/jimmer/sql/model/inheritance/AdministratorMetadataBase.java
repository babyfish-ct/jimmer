package org.babyfish.jimmer.sql.model.inheritance;

import org.babyfish.jimmer.sql.ManyToOne;
import org.babyfish.jimmer.sql.MappedSuperclass;

@MappedSuperclass
public interface AdministratorMetadataBase extends NamedEntity {

    String getEmail();

    String getWebsite();

    @ManyToOne
    Administrator getAdministrator();
}
