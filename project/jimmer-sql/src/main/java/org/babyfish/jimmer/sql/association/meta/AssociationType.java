package org.babyfish.jimmer.sql.association.meta;

import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.meta.impl.AbstractImmutableTypeImpl;
import org.babyfish.jimmer.runtime.DraftContext;
import org.babyfish.jimmer.sql.association.Association;
import org.babyfish.jimmer.impl.util.StaticCache;
import org.babyfish.jimmer.sql.meta.IdGenerator;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.meta.MiddleTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.function.BiFunction;

public class AssociationType extends AbstractImmutableTypeImpl {

    private static final StaticCache<ImmutableProp, AssociationType> CACHE =
            new StaticCache<>(AssociationType::new, false);

    private final ImmutableProp baseProp;

    private final ImmutableType sourceType;

    private final ImmutableType targetType;

    private final AssociationProp sourceProp;

    private final AssociationProp targetProp;

    private final Map<String, ImmutableProp> props;

    public static AssociationType of(ImmutableProp prop) {
        return CACHE.get(prop);
    }

    public static AssociationType of(TypedProp.Association<?, ?> prop) {
        return CACHE.get(prop.unwrap());
    }

    private AssociationType(ImmutableProp baseProp) {

        this.baseProp = baseProp;

        ImmutableProp mappedBy = baseProp.getMappedBy();
        
        if (!(mappedBy != null ? mappedBy : baseProp).isMiddleTableDefinition()) {
            throw new IllegalArgumentException(
                    "\"" +
                            baseProp +
                            "\" is neither association bases on middle table nor " +
                            "inverse association of that"
            );
        }
        sourceType = baseProp.getDeclaringType();
        targetType = baseProp.getTargetType();

        sourceProp = new AssociationProp.Source(this);
        targetProp = new AssociationProp.Target(this);
        Map<String, ImmutableProp> map = new LinkedHashMap<>();
        map.put(sourceProp.getName(), sourceProp);
        map.put(targetProp.getName(), targetProp);
        props = Collections.unmodifiableMap(map);
    }

    public ImmutableProp getBaseProp() {
        return baseProp;
    }

    public ImmutableType getSourceType() {
        return sourceType;
    }

    public ImmutableType getTargetType() {
        return targetType;
    }

    public AssociationProp getSourceProp() {
        return sourceProp;
    }

    public AssociationProp getTargetProp() {
        return targetProp;
    }

    public MiddleTable getMiddleTable(MetadataStrategy strategy) {
        ImmutableProp mappedBy = baseProp.getMappedBy();
        if (mappedBy != null) {
            return mappedBy.<MiddleTable>getStorage(strategy).getInverse();
        }
        return baseProp.getStorage(strategy);
    }

    @NotNull
    @Override
    public Class<?> getJavaClass() {
        return Association.class;
    }

    @Override
    public boolean isKotlinClass() {
        return false;
    }

    @Override
    public boolean isEntity() {
        return true;
    }

    @Override
    public boolean isMappedSuperclass() {
        return false;
    }

    @Override
    public boolean isEmbeddable() {
        return false;
    }

    @NotNull
    @Override
    public Annotation getImmutableAnnotation() { return null; }

    @NotNull
    @Override
    public Map<String, ImmutableProp> getDeclaredProps() {
        return props;
    }

    @NotNull
    @Override
    public Map<String, ImmutableProp> getProps() {
        return props;
    }

    @Override
    public Map<String, ImmutableProp> getSelectableProps() {
        return props;
    }

    @Override
    public Map<String, ImmutableProp> getSelectableReferenceProps() {
        return props;
    }

    @NotNull
    @Override
    public ImmutableProp getProp(String name) {
        ImmutableProp prop = props.get(name);
        if (prop == null) {
            throw new IllegalArgumentException(
                    "There is no property \"" + name + "\" in \"" + this + "\""
            );
        }
        return prop;
    }

    @NotNull
    @Override
    public ImmutableProp getProp(PropId id) {
        int index = id.asIndex();
        if (index == -1) {
            return getProp(id.asName());
        }
        switch (index) {
            case 0:
                return sourceProp;
            case 1:
                return targetProp;
            default:
                throw new IllegalArgumentException(
                    "There is no property whose id is " + id + " in \"" + this + "\""
                );
        }
    }

    @NotNull
    @Override
    public BiFunction<DraftContext, Object, Draft> getDraftFactory() {
        throw new UnsupportedOperationException("draftFactory is not supported by AssociationType");
    }

    @Override
    public boolean isAssignableFrom(ImmutableType type) {
        return false;
    }

    @Override
    public Set<ImmutableType> getSuperTypes() {
        return Collections.emptySet();
    }

    @Override
    public Set<ImmutableType> getAllTypes() {
        return Collections.emptySet();
    }

    @Override
    public ImmutableProp getIdProp() {
        throw new UnsupportedOperationException("Id property is not supported by association type");
    }

    @Nullable
    @Override
    public ImmutableProp getVersionProp() {
        return null;
    }

    @Nullable
    @Override
    public LogicalDeletedInfo getDeclaredLogicalDeletedInfo() {
        return null;
    }

    @Nullable
    @Override
    public LogicalDeletedInfo getLogicalDeletedInfo() {
        return null;
    }

    @NotNull
    @Override
    public Set<ImmutableProp> getKeyProps() {
        return Collections.emptySet();
    }

    @Override
    public String getMicroServiceName() {
        return baseProp.getDeclaringType().getMicroServiceName();
    }

    @Override
    public String getTableName(MetadataStrategy strategy) {
        return getMiddleTable(strategy).getTableName();
    }

    @Override
    public IdGenerator getIdGenerator(MetadataStrategy strategy) {
        return null;
    }

    @Override
    public String toString() {
        return "MiddleTable(" + baseProp + ")";
    }
}
