package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.impl.util.StringUtil;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.meta.DatabaseNamingStrategy;

public class DefaultDatabaseNamingStrategy implements DatabaseNamingStrategy {

    public static final DefaultDatabaseNamingStrategy UPPER_CASE =
            new DefaultDatabaseNamingStrategy(false);

    public static final DefaultDatabaseNamingStrategy LOWER_CASE =
            new DefaultDatabaseNamingStrategy(true);

    private final boolean lowercase;

    protected DefaultDatabaseNamingStrategy(boolean lowercase) {
        this.lowercase = lowercase;
    }

    @Override
    public String tableName(ImmutableType type) {
        return snake(type.getJavaClass().getSimpleName());
    }

    @Override
    public String sequenceName(ImmutableType type) {
        return snake(type.getJavaClass().getSimpleName()) +
                (lowercase ? "_seq_id" : "_SEQ_ID");
    }

    @Override
    public String columnName(ImmutableProp prop) {
        return snake(prop.getName());
    }

    @Override
    public String foreignKeyColumnName(ImmutableProp prop) {
        return snake(prop.getName()) +
                (lowercase ? "_id" : "_ID");
    }

    @Override
    public String middleTableName(ImmutableProp prop) {
        return snake(prop.getDeclaringType().getJavaClass().getSimpleName()) +
                '_' +
                snake(prop.getTargetType().getJavaClass().getSimpleName()) +
                (lowercase ? "_mapping" : "_MAPPING");
    }

    @Override
    public String middleTableBackRefColumnName(ImmutableProp prop) {
        return snake(prop.getDeclaringType().getJavaClass().getSimpleName()) +
                (lowercase ? "_id" : "_ID");
    }

    @Override
    public String middleTableTargetRefColumnName(ImmutableProp prop) {
        return snake(prop.getTargetType().getJavaClass().getSimpleName()) +
                (lowercase ? "_id" : "_ID");
    }

    protected String snake(String text) {
        return StringUtil.snake(text, lowercase ? StringUtil.SnakeCase.LOWER : StringUtil.SnakeCase.UPPER);
    }
}
