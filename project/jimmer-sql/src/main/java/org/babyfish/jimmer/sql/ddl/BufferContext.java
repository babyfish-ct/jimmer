package org.babyfish.jimmer.sql.ddl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ddl.annotations.TableDef;
import org.babyfish.jimmer.sql.ddl.fake.FakeImmutableTypeImpl;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.util.*;

/**
 * @author honhimW
 */

class BufferContext {

    public final StringBuilder buf;

    public final JSqlClientImplementor client;

    public final ImmutableType tableType;

    public Map<String, ImmutableProp> allDefinitionProps;

    public List<ImmutableProp> definitionProps;

    public final String tableName;

    public final List<String> commentStatements;

    private TableDef tableDef;
    
    private final Map<Class<? extends ConstraintNamingStrategy>, ConstraintNamingStrategy> namingStrategies;

    public BufferContext(JSqlClientImplementor client, ImmutableType tableType) {
        this.buf = new StringBuilder();
        this.client = client;
        this.tableType = tableType;
        this.tableName = tableType.getTableName(client.getMetadataStrategy());
        this.commentStatements = new ArrayList<>();
        this.namingStrategies = new HashMap<>();
        this.definitionProps = new ArrayList<>();
        this.allDefinitionProps = DDLUtils.allDefinitionProps(tableType);
        this.definitionProps.addAll(this.allDefinitionProps.values());
        if (tableType instanceof FakeImmutableTypeImpl) {
            FakeImmutableTypeImpl fake = (FakeImmutableTypeImpl) tableType;
            tableDef = fake.tableDef;
        } else {
            Class<?> javaClass = tableType.getJavaClass();
            if (javaClass.isAnnotationPresent(TableDef.class)) {
                tableDef = javaClass.getAnnotation(TableDef.class);
            }
        }
    }

    public Optional<TableDef> getTableDef() {
        return Optional.ofNullable(tableDef);
    }

    public ConstraintNamingStrategy getNamingStrategy(Class<? extends ConstraintNamingStrategy> namingStrategy) {
        return this.namingStrategies.compute(namingStrategy, (aClass, ns) -> {
            if (ns == null) {
                try {
                    ns = aClass.getConstructor().newInstance();
                } catch (Exception e) {
                    throw new IllegalArgumentException("NamingStrategy doesn't have a no-arg constructor");
                }
            }
            return ns;
        });
    }

}
