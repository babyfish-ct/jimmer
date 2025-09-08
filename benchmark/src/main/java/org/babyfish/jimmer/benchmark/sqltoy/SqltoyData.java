package org.babyfish.jimmer.benchmark.sqltoy;

import org.sagacity.sqltoy.config.annotation.Column;
import org.sagacity.sqltoy.config.annotation.Entity;
import org.sagacity.sqltoy.config.annotation.Id;

import java.io.Serializable;

@Entity(tableName = "DATA")
public class SqltoyData implements Serializable {

    @Id
    private long id;

    @Column(name = "VALUE_1")
    private String value1;
    @Column(name = "VALUE_2")
    private String value2;
    @Column(name = "VALUE_3")
    private String value3;
    @Column(name = "VALUE_4")
    private String value4;
    @Column(name = "VALUE_5")
    private String value5;
    @Column(name = "VALUE_6")
    private String value6;
    @Column(name = "VALUE_7")
    private String value7;
    @Column(name = "VALUE_8")
    private String value8;
    @Column(name = "VALUE_9")
    private String value9;

    public long getId() {
        return id;
    }

    public String getValue1() {
        return value1;
    }

    public String getValue2() {
        return value2;
    }

    public String getValue3() {
        return value3;
    }

    public String getValue4() {
        return value4;
    }

    public String getValue5() {
        return value5;
    }

    public String getValue6() {
        return value6;
    }

    public String getValue7() {
        return value7;
    }

    public String getValue8() {
        return value8;
    }

    public String getValue9() {
        return value9;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setValue1(String value1) {
        this.value1 = value1;
    }

    public void setValue2(String value2) {
        this.value2 = value2;
    }

    public void setValue3(String value3) {
        this.value3 = value3;
    }

    public void setValue4(String value4) {
        this.value4 = value4;
    }

    public void setValue5(String value5) {
        this.value5 = value5;
    }

    public void setValue6(String value6) {
        this.value6 = value6;
    }

    public void setValue7(String value7) {
        this.value7 = value7;
    }

    public void setValue8(String value8) {
        this.value8 = value8;
    }

    public void setValue9(String value9) {
        this.value9 = value9;
    }
}
