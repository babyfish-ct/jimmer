package org.babyfish.jimmer.sql.ddl.fake;

import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.DraftContext;
import org.babyfish.jimmer.sql.ddl.annotations.TableDef;
import org.babyfish.jimmer.sql.meta.IdGenerator;
import org.babyfish.jimmer.sql.meta.LogicalDeletedValueGenerator;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.meta.SqlContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * @author honhimW
 */
 
public class FakeImmutableTypeImpl implements ImmutableType {

    public TableDef tableDef;

    public Class<?> javaClass;

    @Override
    public @NotNull Class<?> getJavaClass() {
        return javaClass;
    }

    public boolean isKotlinClass;

    @Override
    public boolean isKotlinClass() {
        return isKotlinClass;
    }

    public boolean isEntity;

    @Override
    public boolean isEntity() {
        return isEntity;
    }

    public boolean isMappedSuperclass;

    @Override
    public boolean isMappedSuperclass() {
        return isMappedSuperclass;
    }

    public boolean isEmbeddable;

    @Override
    public boolean isEmbeddable() {
        return isEmbeddable;
    }

    public Annotation immutableAnnotation;

    @Override
    public @NotNull Annotation getImmutableAnnotation() {
        return immutableAnnotation;
    }

    public boolean isAssignableFrom;

    @Override
    public boolean isAssignableFrom(ImmutableType type) {
        return isAssignableFrom;
    }

    public ImmutableType primarySuperType;

    @Override
    public @Nullable ImmutableType getPrimarySuperType() {
        return primarySuperType;
    }

    public Set<ImmutableType> superTypes;

    @Override
    public Set<ImmutableType> getSuperTypes() {
        return superTypes;
    }

    public Set<ImmutableType> allTypes;

    @Override
    public Set<ImmutableType> getAllTypes() {
        return allTypes;
    }

    public BiFunction<DraftContext, Object, Draft> draftFactory;

    @Override
    public @NotNull BiFunction<DraftContext, Object, Draft> getDraftFactory() {
        return draftFactory;
    }

    public Map<String, ImmutableProp> declaredProps;

    @Override
    public @NotNull Map<String, ImmutableProp> getDeclaredProps() {
        return declaredProps;
    }

    public ImmutableProp idProp;

    @Override
    public ImmutableProp getIdProp() {
        return idProp;
    }

    public ImmutableProp versionProp;

    @Override
    public @Nullable ImmutableProp getVersionProp() {
        return versionProp;
    }

    public LogicalDeletedInfo declaredLogicalDeletedInfo;

    @Override
    public @Nullable LogicalDeletedInfo getDeclaredLogicalDeletedInfo() {
        return declaredLogicalDeletedInfo;
    }

    public LogicalDeletedInfo logicalDeletedInfo;

    @Override
    public @Nullable LogicalDeletedInfo getLogicalDeletedInfo() {
        return logicalDeletedInfo;
    }

    public KeyMatcher keyMatcher;

    @Override
    public @NotNull KeyMatcher getKeyMatcher() {
        return keyMatcher;
    }

    public Map<String, ImmutableProp> props;

    @Override
    public @NotNull Map<String, ImmutableProp> getProps() {
        return props;
    }

    public Map<String, ImmutableProp> entityProps;

    @Override
    public @NotNull Map<String, ImmutableProp> getEntityProps() {
        return entityProps;
    }

    @Override
    public @NotNull ImmutableProp getProp(String name) {
        return props.get(name);
    }

    @Override
    public @NotNull ImmutableProp getProp(PropId id) {
        return props.get(id.asName());
    }

    public Map<String, List<ImmutableProp>> embeddedPaths;

    @Override
    public Map<String, List<ImmutableProp>> getEmbeddedPaths() {
        return embeddedPaths;
    }

    public Map<String, ImmutableProp> selectableProps;

    @Override
    public Map<String, ImmutableProp> getSelectableProps() {
        return selectableProps;
    }

    public Map<String, ImmutableProp> selectableScalarProps;

    @Override
    public Map<String, ImmutableProp> getSelectableScalarProps() {
        return selectableScalarProps;
    }

    public Map<String, ImmutableProp> selectableReferenceProps;

    @Override
    public Map<String, ImmutableProp> getSelectableReferenceProps() {
        return selectableReferenceProps;
    }

    public Map<String, ImmutableProp> referenceProps;

    @Override
    public Map<String, ImmutableProp> getReferenceProps() {
        return referenceProps;
    }

    public Map<String, ImmutableProp> objectCacheProps;

    @Override
    public Map<String, ImmutableProp> getObjectCacheProps() {
        return objectCacheProps;
    }

    public String microServiceName;

    @Override
    public String getMicroServiceName() {
        return microServiceName;
    }

    public String tableName;

    @Override
    public String getTableName(MetadataStrategy strategy) {
        return tableName;
    }

    public IdGenerator idGenerator;

    @Override
    public IdGenerator getIdGenerator(SqlContext sqlContext) {
        return idGenerator;
    }

    public LogicalDeletedValueGenerator<?> logicalDeletedValueGenerator;

    @Override
    public LogicalDeletedValueGenerator<?> getLogicalDeletedValueGenerator(SqlContext sqlContext) {
        return logicalDeletedValueGenerator;
    }

    public List<ImmutableProp> propChain;

    @Override
    public List<ImmutableProp> getPropChain(String columnName, MetadataStrategy strategy, boolean nullable) {
        return propChain;
    }
}
