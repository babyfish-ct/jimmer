package org.babyfish.jimmer.benchmark.objsql;

import com.github.braisdom.objsql.Databases;
import com.github.braisdom.objsql.Query;
import com.github.braisdom.objsql.QueryFactory;
import com.github.braisdom.objsql.annotations.Column;
import com.github.braisdom.objsql.annotations.DomainModel;
import com.github.braisdom.objsql.annotations.PrimaryKey;

import java.sql.SQLException;
import java.util.List;

@DomainModel(tableName = "DATA")
public class ObjSqlData {

    //-------------------------------------
    // Annotation processor of ObjectiveSql cannot work with java 16
    // Hardcode necessary generated code manually.

    public static List<ObjSqlData> queryAll() throws SQLException {
        Query<ObjSqlData> query = createQuery();
        query.where("");
        return query.execute();
    }

    public static Query<ObjSqlData> createQuery() {
        QueryFactory queryFactory = Databases.getQueryFactory();
        return queryFactory.createQuery(ObjSqlData.class);
    }

    //-------------------------------------

    @PrimaryKey
    private long id;

    @Column(name = "VALUE_1")
    private int value1;

    @Column(name = "VALUE_2")
    private int value2;

    @Column(name = "VALUE_3")
    private int value3;

    @Column(name = "VALUE_4")
    private int value4;

    @Column(name = "VALUE_5")
    private int value5;

    @Column(name = "VALUE_6")
    private int value6;

    @Column(name = "VALUE_7")
    private int value7;

    @Column(name = "VALUE_8")
    private int value8;

    @Column(name = "VALUE_9")
    private int value9;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getValue1() {
        return value1;
    }

    public void setValue1(int value1) {
        this.value1 = value1;
    }

    public int getValue2() {
        return value2;
    }

    public void setValue2(int value2) {
        this.value2 = value2;
    }

    public int getValue3() {
        return value3;
    }

    public void setValue3(int value3) {
        this.value3 = value3;
    }

    public int getValue4() {
        return value4;
    }

    public void setValue4(int value4) {
        this.value4 = value4;
    }

    public int getValue5() {
        return value5;
    }

    public void setValue5(int value5) {
        this.value5 = value5;
    }

    public int getValue6() {
        return value6;
    }

    public void setValue6(int value6) {
        this.value6 = value6;
    }

    public int getValue7() {
        return value7;
    }

    public void setValue7(int value7) {
        this.value7 = value7;
    }

    public int getValue8() {
        return value8;
    }

    public void setValue8(int value8) {
        this.value8 = value8;
    }

    public int getValue9() {
        return value9;
    }

    public void setValue9(int value9) {
        this.value9 = value9;
    }
}