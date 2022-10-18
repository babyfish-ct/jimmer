package org.babyfish.jimmer.sql.model.inheritance;

import org.babyfish.jimmer.sql.*;

@MappedSuperclass
public interface AdministratorMetadataBase extends NamedEntity {

    String getEmail();

    String getWebsite();

    @OneToOne
    @OnDissociate(DissociateAction.DELETE)
    Administrator getAdministrator();
}
