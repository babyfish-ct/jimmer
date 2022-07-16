package org.babyfish.jimmer.benchmark.jooq;

import org.jooq.DataType;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.TableField;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

public class DataTable extends TableImpl<Record> {

    public static final DataTable DATA = new DataTable();

    public DataTable() {
        super(DSL.name("DATA"));
    }

    public final TableField<Record, Long> ID =
            createField(DSL.name("ID"), SQLDataType.BIGINT, this);

    public final TableField<Record, Integer> VALUE_1 =
            createField(DSL.name("VALUE_1"), SQLDataType.INTEGER, this);

    public final TableField<Record, Integer> VALUE_2 =
            createField(DSL.name("VALUE_2"), SQLDataType.INTEGER, this);

    public final TableField<Record, Integer> VALUE_3 =
            createField(DSL.name("VALUE_3"), SQLDataType.INTEGER, this);

    public final TableField<Record, Integer> VALUE_4 =
            createField(DSL.name("VALUE_4"), SQLDataType.INTEGER, this);

    public final TableField<Record, Integer> VALUE_5 =
            createField(DSL.name("VALUE_5"), SQLDataType.INTEGER, this);

    public final TableField<Record, Integer> VALUE_6 =
            createField(DSL.name("VALUE_6"), SQLDataType.INTEGER, this);

    public final TableField<Record, Integer> VALUE_7 =
            createField(DSL.name("VALUE_7"), SQLDataType.INTEGER, this);

    public final TableField<Record, Integer> VALUE_8 =
            createField(DSL.name("VALUE_8"), SQLDataType.INTEGER, this);

    public final TableField<Record, Integer> VALUE_9 =
            createField(DSL.name("VALUE_9"), SQLDataType.INTEGER, this);
}
