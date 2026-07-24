package org.babyfish.jimmer.sql.model.generic;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Table;

@Entity
@Table(name = "GENERIC_TREE_NODE")
public interface GenericTreeNode extends GenericBaseTreeEntity<GenericTreeNode> {

    @Id
    long getId();

    String getName();
}
