package org.babyfish.jimmer.sql.ddl;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.babyfish.jimmer.meta.EmbeddedLevel;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.GeneratedValue;
import org.babyfish.jimmer.sql.GenerationType;
import org.babyfish.jimmer.sql.ddl.annotations.*;
import org.babyfish.jimmer.sql.ddl.dialect.DDLDialect;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.meta.SingleColumn;
import org.babyfish.jimmer.sql.meta.Storage;
import org.babyfish.jimmer.sql.meta.UserIdGenerator;
import org.babyfish.jimmer.sql.meta.impl.Storages;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.sql.Types.*;

/**
 * @author honhimW
 */

public class DDLUtils {

    public static String replace(String type, Long length, Integer precision, Integer scale) {
        if (scale != null) {
            type = StringUtils.replaceOnce(type, "$s", scale.toString());
        }
        if (length != null) {
            type = StringUtils.replaceOnce(type, "$l", length.toString());
        }
        if (precision != null) {
            type = StringUtils.replaceOnce(type, "$p", precision.toString());
        }
        return type;
    }

    public static boolean isTemporal(int jdbcType) {
        switch (jdbcType) {
            case TIME:
            case TIME_WITH_TIMEZONE:
            case DATE:
            case TIMESTAMP:
            case TIMESTAMP_WITH_TIMEZONE:
                return true;
            default:
                return false;
        }
    }

    @Nullable
    public static Integer resolveDefaultPrecision(int jdbcType, @Nonnull DDLDialect dialect) {
        Integer precision = null;
        if (isTemporal(jdbcType)) {
            precision = dialect.getDefaultTimestampPrecision(jdbcType);
        }
        if (jdbcType == DECIMAL) {
            precision = dialect.getDefaultDecimalPrecision(jdbcType);
        }
        if (jdbcType == FLOAT) {
            precision = dialect.getDefaultDecimalPrecision(jdbcType);
        }
        if (jdbcType == DOUBLE) {
            precision = dialect.getDefaultDecimalPrecision(jdbcType);
        }
        return precision;
    }

    public static String getName(ImmutableProp prop, MetadataStrategy metadataStrategy) {
        Storage storage = Storages.of(prop, metadataStrategy);
        if (storage instanceof SingleColumn) {
            SingleColumn singleColumn = (SingleColumn) storage;
            return singleColumn.getName();
        }
        return prop.getName();
    }

    public static Map<String, ImmutableProp> allDefinitionProps(ImmutableType immutableType) {
        Map<String, ImmutableProp> props = new LinkedHashMap<>();
        Map<String, ImmutableProp> selectableScalarProps = immutableType.getSelectableProps();
        selectableScalarProps.forEach((k, v) -> {
            if (v.isEmbedded(EmbeddedLevel.BOTH)) {
                ImmutableType targetType = v.getTargetType();
                Map<String, ImmutableProp> next = allDefinitionProps(targetType);
                next.forEach((nextKey, nextValue) -> props.put(k + '.' + nextKey, nextValue));
            } else {
                props.put(k, v);
            }
        });
        return props;
    }

    public static List<ForeignKey> getForeignKeys(MetadataStrategy metadataStrategy, ImmutableType immutableType) {
        List<ForeignKey> foreignKeys = new ArrayList<>();
        Map<String, ImmutableProp> allDefinitionProps = DDLUtils.allDefinitionProps(immutableType);
        for (Map.Entry<String, ImmutableProp> entry : allDefinitionProps.entrySet()) {
            ImmutableProp definitionProps = entry.getValue();
            if (definitionProps.isTargetForeignKeyReal(metadataStrategy)) {
                ColumnDef columnDef = definitionProps.getAnnotation(ColumnDef.class);
                org.babyfish.jimmer.sql.ddl.annotations.ForeignKey foreignKey;
                if (columnDef != null) {
                    foreignKey = columnDef.foreignKey();
                } else {
                    foreignKey = new DefaultForeignKey();
                }
                ForeignKey _foreignKey = new ForeignKey(foreignKey, definitionProps, immutableType, definitionProps.getTargetType());
                foreignKeys.add(_foreignKey);
            }
        }
        return foreignKeys;
    }

    public static class DefaultColumnDef implements ColumnDef {

        @Override
        public Nullable nullable() {
            return Nullable.NULL;
        }

        @Override
        public String sqlType() {
            return "";
        }

        @Override
        public int jdbcType() {
            return OTHER;
        }

        @Override
        public long length() {
            return -1;
        }

        @Override
        public int precision() {
            return -1;
        }

        @Override
        public int scale() {
            return -1;
        }

        @Override
        public String comment() {
            return "";
        }

        @Override
        public String definition() {
            return "";
        }

        @Override
        public org.babyfish.jimmer.sql.ddl.annotations.ForeignKey foreignKey() {
            return new DefaultForeignKey();
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return ColumnDef.class;
        }
    }

    public static class DefaultForeignKey implements org.babyfish.jimmer.sql.ddl.annotations.ForeignKey {
        @Override
        public String name() {
            return "";
        }

        @Override
        public String definition() {
            return "";
        }

        @Override
        public OnDeleteAction action() {
            return OnDeleteAction.NONE;
        }

        @Override
        public Class<? extends ConstraintNamingStrategy> naming() {
            return ConstraintNamingStrategy.class;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return org.babyfish.jimmer.sql.ddl.annotations.ForeignKey.class;
        }
    }

    public static class DefaultGeneratedValue implements GeneratedValue {
        @Override
        public GenerationType strategy() {
            return GenerationType.AUTO;
        }

        @Override
        public Class<? extends UserIdGenerator<?>> generatorType() {
            return UserIdGenerator.None.class;
        }

        @Override
        public String generatorRef() {
            return "";
        }

        @Override
        public String sequenceName() {
            return "";
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return GeneratedValue.class;
        }
    }

    public static class DefaultTableDef implements TableDef {
        @Override
        public Unique[] uniques() {
            return new Unique[0];
        }

        @Override
        public Index[] indexes() {
            return new Index[0];
        }

        @Override
        public String comment() {
            return "";
        }

        @Override
        public Check[] checks() {
            return new Check[0];
        }

        @Override
        public String tableType() {
            return "";
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return TableDef.class;
        }
    }

    public static abstract class DefaultUnique implements Unique {
        @Override
        public String name() {
            return "";
        }

        @Override
        public Kind kind() {
            return Kind.PATH;
        }

        @Override
        public Class<? extends ConstraintNamingStrategy> naming() {
            return ConstraintNamingStrategy.class;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return Unique.class;
        }
    }

}
