package org.babyfish.jimmer.sql.model.ld;

import org.babyfish.jimmer.sql.*;

import java.util.List;

@Entity
@Table(name = "category_2")
public interface Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    String name();

    @LogicalDeleted
    long deletedMillis();

    @ManyToMany(mappedBy = "categories")
    List<Post> posts();
}
