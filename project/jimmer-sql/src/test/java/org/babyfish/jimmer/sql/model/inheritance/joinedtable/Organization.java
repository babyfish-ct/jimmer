package org.babyfish.jimmer.sql.model.inheritance.joinedtable;

import org.babyfish.jimmer.sql.DiscriminatorValue;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.OneToMany;
import org.babyfish.jimmer.sql.Table;

import java.util.List;

@Entity
@Table(name = "JOINED_ORGANIZATION")
@DiscriminatorValue("ORG")
public interface Organization extends Client {

    String taxCode();

    @OneToMany(mappedBy = "organization")
    List<OrganizationProject> projects();
}
