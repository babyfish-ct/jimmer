package org.babyfish.jimmer.sql.model.ld;

import org.babyfish.jimmer.sql.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "post_2")
public interface Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    String name();

    @LogicalDeleted
    long deletedMillis();

    @ManyToMany
    @JoinTable(
            name = "post_2_category_2_mapping",
            joinColumns = @JoinColumn(foreignKeyType = ForeignKeyType.FAKE),
            inverseJoinColumns = @JoinColumn(foreignKeyType = ForeignKeyType.FAKE),
            logicalDeletedFilter = @JoinTable.LogicalDeletedFilter(
                    columnName = "DELETED_UUID",
                    type = UUID.class
            )
    )
    List<Category> categories();
}
