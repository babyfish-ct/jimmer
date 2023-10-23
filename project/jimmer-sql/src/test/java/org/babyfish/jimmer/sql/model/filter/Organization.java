package org.babyfish.jimmer.sql.model.filter;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.ManyToOne;
import org.babyfish.jimmer.sql.OneToMany;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Entity
public interface Organization {

    @Id
    long id();

    String name();

    String tenant();

    @ManyToOne
    @Nullable
    Organization parent();

    @OneToMany(mappedBy = "parent")
    List<Organization> childOrganizations();
}
