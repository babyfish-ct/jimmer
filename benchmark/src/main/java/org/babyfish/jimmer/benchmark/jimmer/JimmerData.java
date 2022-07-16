package org.babyfish.jimmer.benchmark.jimmer;

import org.springframework.data.repository.NoRepositoryBean;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "DATA")
public interface JimmerData {

    @Id
    long id();

    @Column(name = "VALUE_1", nullable = false)
    int value1();

    @Column(name = "VALUE_2", nullable = false)
    int value2();

    @Column(name = "VALUE_3", nullable = false)
    int value3();

    @Column(name = "VALUE_4", nullable = false)
    int value4();

    @Column(name = "VALUE_5", nullable = false)
    int value5();

    @Column(name = "VALUE_6", nullable = false)
    int value6();

    @Column(name = "VALUE_7", nullable = false)
    int value7();

    @Column(name = "VALUE_8", nullable = false)
    int value8();

    @Column(name = "VALUE_9", nullable = false)
    int value9();
}
