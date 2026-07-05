package org.babyfish.jimmer.sql.model.inheritance.joinedtable;

import org.babyfish.jimmer.sql.*;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Entity
@Table(name = "JOINED_ORGANIZATION")
@DiscriminatorValue("ORG")
public interface Organization extends Client {

    String taxCode();

    @Nullable
    @DatabaseDefault("'DEFAULT_ORGANIZATION_STATUS'")
    String status();

    @OneToMany(mappedBy = "organization")
    List<OrganizationProject> projects();
}
