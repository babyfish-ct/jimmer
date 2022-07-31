package org.babyfish.jimmer.benchmark.jimmer;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Table;

@Entity
@Table(name = "DATA")
public interface JimmerData {

    @Id
    long id();

    @Column(name = "VALUE_1")
    int value1();

    @Column(name = "VALUE_2")
    int value2();

    @Column(name = "VALUE_3")
    int value3();

    @Column(name = "VALUE_4")
    int value4();

    @Column(name = "VALUE_5")
    int value5();

    @Column(name = "VALUE_6")
    int value6();

    @Column(name = "VALUE_7")
    int value7();

    @Column(name = "VALUE_8")
    int value8();

    @Column(name = "VALUE_9")
    int value9();
}
