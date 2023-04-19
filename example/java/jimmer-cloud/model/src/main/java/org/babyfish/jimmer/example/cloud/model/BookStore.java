package org.babyfish.jimmer.example.cloud.model;

import org.babyfish.jimmer.example.cloud.model.common.BaseEntity;
import org.babyfish.jimmer.sql.*;

import javax.validation.constraints.Null;
import java.util.List;

@Entity(microServiceName = "store-service")
public interface BookStore extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Key
    String name();

    @Null
    String website();

    @OneToMany(mappedBy = "store")
    List<Book> books();
}
