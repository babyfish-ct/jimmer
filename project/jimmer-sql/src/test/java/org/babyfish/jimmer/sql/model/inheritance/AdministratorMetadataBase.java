package org.babyfish.jimmer.sql.model.inheritance;

import org.babyfish.jimmer.pojo.Static;
import org.babyfish.jimmer.sql.*;
import org.jetbrains.annotations.Nullable;

@MappedSuperclass
public interface AdministratorMetadataBase extends NamedEntity {

    String getEmail();

    String getWebsite();

    @OneToOne(inputNotNull = true)
    @Nullable
    @OnDissociate(DissociateAction.DELETE)
    @Static(alias = "default", name="administratorId", idOnly = true)
    @Static(alias = "composite")
    Administrator getAdministrator();
}
