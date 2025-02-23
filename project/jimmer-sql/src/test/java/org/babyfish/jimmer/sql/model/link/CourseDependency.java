package org.babyfish.jimmer.sql.model.link;

import org.babyfish.jimmer.sql.*;

@Entity
public interface CourseDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @ManyToOne
    @OnDissociate(DissociateAction.DELETE)
    Course nextCourse();

    @ManyToOne
    @OnDissociate(DissociateAction.DELETE)
    Course prevCourse();

    String reason();
}
