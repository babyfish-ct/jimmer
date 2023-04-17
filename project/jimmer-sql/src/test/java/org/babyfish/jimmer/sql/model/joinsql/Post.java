package org.babyfish.jimmer.sql.model.joinsql;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.JoinSql;
import org.babyfish.jimmer.sql.ManyToMany;

import java.util.List;

@Entity
public interface Post {

    @Id
    long id();
    
    String name();
    
    @ManyToMany
    @JoinSql("contains_id(%alias.category_ids, %target_alias.id)")
    List<Category> categories();
}
