package org.babyfish.jimmer.sql.model.issue888;

import org.babyfish.jimmer.sql.*;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Entity
@Table(name = "issue888_item")
public interface Item {

    @Id
    long id();

    @Column
    String name();

    @Nullable
    @ManyToOne
    Structure structure();

    @Nullable
    @ManyToOne
    Item parent();

    @OneToMany(
            mappedBy = "parent",
            orderedProps = @OrderedProp("id")
    )
    List<Item> childItems();
}
