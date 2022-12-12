package org.babyfish.jimmer.sql.model.inheritance;

import org.babyfish.jimmer.sql.*;
import org.jetbrains.annotations.Nullable;

@MappedSuperclass
public interface AdministratorMetadataBase extends NamedEntity {

    String getEmail();

    String getWebsite();

    @OneToOne
    @Nullable
    @OnDissociate(DissociateAction.DELETE)
    Administrator getAdministrator();
}
