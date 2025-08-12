package org.babyfish.jimmer.benchmark.mug;

import com.google.mu.safesql.SqlName;

/**
 * @author Enaium
 */
public class MugSafeSqlData {

    private final long id;
    private final String value1;
    private final String value2;
    private final String value3;
    private final String value4;
    private final String value5;
    private final String value6;
    private final String value7;
    private final String value8;
    private final String value9;

    public MugSafeSqlData(@SqlName("ID") long id, @SqlName("VALUE_1") String value1, @SqlName("VALUE_2") String value2, @SqlName("VALUE_3") String value3, @SqlName("VALUE_4") String value4, @SqlName("VALUE_5") String value5, @SqlName("VALUE_6") String value6, @SqlName("VALUE_7") String value7, @SqlName("VALUE_8") String value8, @SqlName("VALUE_9") String value9) {
        this.id = id;
        this.value1 = value1;
        this.value2 = value2;
        this.value3 = value3;
        this.value4 = value4;
        this.value5 = value5;
        this.value6 = value6;
        this.value7 = value7;
        this.value8 = value8;
        this.value9 = value9;
    }

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
}
