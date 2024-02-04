package org.babyfish.jimmer.sql.meta.impl;

import org.babyfish.jimmer.impl.util.GenericValidator;
import org.babyfish.jimmer.meta.LogicalDeletedInfo;
import org.babyfish.jimmer.meta.ModelException;
import org.babyfish.jimmer.sql.LogicalDeleted;
import org.babyfish.jimmer.sql.meta.LogicalDeletedValueGenerator;
import org.babyfish.jimmer.sql.meta.SqlContext;

public class LogicalDeletedValueGenerators {

    public static LogicalDeletedValueGenerator<?> of(LogicalDeletedInfo logicalDeletedInfo, SqlContext sqlContext) {

        if (logicalDeletedInfo == null) {
            return null;
        }

        Class<? extends LogicalDeletedValueGenerator<?>> generatorType = logicalDeletedInfo.getGeneratorType();
        if (generatorType != null) {
            try {
                return sqlContext.getLogicalDeletedValueGenerator(generatorType);
            } catch (Exception e) {
                throw new ModelException(
                        "Cannot instance of \"" +
                                generatorType.getName() +
                                "\" required by \"" +
                                logicalDeletedInfo.getProp() +
                                "\""
                );
            }
        }

        String generatorRef = logicalDeletedInfo.getGeneratorRef();
        if (generatorRef != null) {
            LogicalDeletedValueGenerator<?> generator;
            try {
                generator = sqlContext.getLogicalDeletedValueGenerator(generatorRef);
            } catch (Exception e) {
                throw new ModelException(
                        "Cannot instance of \"" +
                                generatorType.getName() +
                                "\" required by \"" +
                                logicalDeletedInfo.getProp() +
                                "\""
                );
            }
            new GenericValidator(logicalDeletedInfo.getProp(), LogicalDeleted.class, generator.getClass(), LogicalDeletedValueGenerator.class)
                    .expect(0, logicalDeletedInfo.getProp().getReturnClass())
                    .validate();
            return generator;
        }

        return new LogicalDeletedValueGenerator<Object>() {
            @Override
            public Object generate() {
                return logicalDeletedInfo.generateValue();
            }
        };
    }
}
