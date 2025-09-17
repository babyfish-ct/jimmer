package org.babyfish.jimmer.sql.model.ld;

import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

@Entity
@Table(name = "post2_item")
public interface PostItem {

    @Id
    long id();

    String name();

    @LogicalDeleted
    UUID deletedUUID();

    @ManyToOne
    @OnDissociate(DissociateAction.SET_NULL)
    @Nullable
    Post post();
}
