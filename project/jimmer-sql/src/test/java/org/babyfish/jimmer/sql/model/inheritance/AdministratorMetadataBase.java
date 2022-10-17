package org.babyfish.jimmer.sql.model.inheritance;

import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ManyToOne;
import org.babyfish.jimmer.sql.MappedSuperclass;
import org.babyfish.jimmer.sql.OnDissociate;

@MappedSuperclass
public interface AdministratorMetadataBase extends NamedEntity {

    String getEmail();

    String getWebsite();

    @ManyToOne(unique = true)
    @OnDissociate(DissociateAction.DELETE)
    Administrator getAdministrator();
}
