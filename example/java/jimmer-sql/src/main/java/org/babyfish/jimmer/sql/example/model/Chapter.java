package org.babyfish.jimmer.sql.example.model;

import org.babyfish.jimmer.pojo.AutoScalarStrategy;
import org.babyfish.jimmer.pojo.StaticType;
import org.babyfish.jimmer.sql.*;

@Entity
@StaticType(alias = "forCompositeBookInput", autoScalarStrategy = AutoScalarStrategy.DECLARED)
public interface Chapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @ManyToOne
    @Key
    @OnDissociate(DissociateAction.DELETE)
    Book book();

    @Key
    @Column(name = "chapter_no")
    int index();

    String title();
}
