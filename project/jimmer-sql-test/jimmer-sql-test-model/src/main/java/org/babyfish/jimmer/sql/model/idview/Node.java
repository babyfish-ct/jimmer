package org.babyfish.jimmer.sql.model.idview;

import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.util.List;

@Entity
@Table(name = "ID_VIEW_NODE")
public interface Node {

    @Id
    int id();

    @Nullable
    @ManyToOne
    Node parent();

    @Nullable
    @IdView("parent")
    Integer parentKey();

    @OneToMany(mappedBy = "parent")
    List<Node> children();
}
