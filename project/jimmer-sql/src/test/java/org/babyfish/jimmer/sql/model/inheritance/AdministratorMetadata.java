package org.babyfish.jimmer.sql.model.inheritance;

import org.babyfish.jimmer.pojo.StaticType;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;

@Entity
@StaticType(alias = "default", topLevelName = "AdminMetadataInput")
@StaticType(alias = "composite", topLevelName = "CompositeAdminMetadataInput")
public interface AdministratorMetadata extends AdministratorMetadataBase {

    @Id
    long getId();
}
