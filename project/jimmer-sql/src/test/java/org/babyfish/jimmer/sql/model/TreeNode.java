package org.babyfish.jimmer.sql.model;

import org.babyfish.jimmer.sql.meta.UUIDIdGenerator;

import javax.persistence.*;
import java.util.List;
import java.util.UUID;

@Entity
public interface TreeNode {

    @Id
    @GeneratedValue(generator = UUIDIdGenerator.FULL_NAME)
    UUID id();

    String name();

    @ManyToOne
    TreeNode parent();

    @OneToMany(mappedBy = "parent")
    List<TreeNode> childNodes();
}
