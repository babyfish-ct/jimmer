package org.babyfish.jimmer.sql.model.filter;

import org.babyfish.jimmer.sql.*;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Entity
@KeyUniqueConstraint(isNullNotDistinct = true)
public interface File {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Key
    String name();

    @ManyToOne
    @Key
    @Nullable
    @OnDissociate(DissociateAction.DELETE)
    File parent();

    @OneToMany(mappedBy = "parent", orderedProps = @OrderedProp("id"))
    List<File> childFiles();

    @ManyToMany
    @JoinTable(deletedWhenEndpointIsLogicallyDeleted = true)
    List<User> users();
}
