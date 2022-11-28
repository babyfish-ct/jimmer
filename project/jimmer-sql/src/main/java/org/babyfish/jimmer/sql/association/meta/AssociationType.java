package org.babyfish.jimmer.sql.association.meta;

import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.meta.impl.DatabaseIdentifiers;
import org.babyfish.jimmer.runtime.DraftContext;
import org.babyfish.jimmer.sql.association.Association;
import org.babyfish.jimmer.sql.meta.Column;
import org.babyfish.jimmer.sql.meta.IdGenerator;
import org.babyfish.jimmer.sql.meta.MiddleTable;
import org.babyfish.jimmer.impl.util.StaticCache;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.function.BiFunction;

public class AssociationType implements ImmutableType {

    private static final StaticCache<ImmutableProp, AssociationType> CACHE =
            new StaticCache<>(AssociationType::new, false);

    private MiddleTable middleTable;

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

        if (mappedBy != null && mappedBy.getStorage() instanceof MiddleTable) {
            middleTable = mappedBy.<MiddleTable>getStorage().getInverse();
        } else if (baseProp.getStorage() instanceof MiddleTable){
            middleTable = baseProp.getStorage();
        }

        if (middleTable == null) {
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

    public MiddleTable getMiddleTable() {
        return middleTable;
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
    public Annotation getImmutableAnnotation() { return null; }

    @Override
    public String getTableName() {
        return middleTable.getTableName();
    }

    @Override
    public Map<String, ImmutableProp> getDeclaredProps() {
        return props;
    }

    @Override
    public Map<String, ImmutableProp> getProps() {
        return props;
    }

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

    @Override
    public ImmutableProp getProp(int id) {
        switch (id) {
            case 1:
                return sourceProp;
            case 2:
                return targetProp;
            default:
                throw new IllegalArgumentException(
                    "There is no property whose id is " + id + " in \"" + this + "\""
                );
        }
    }

    @Override
    public ImmutableProp getPropByColumnName(String columnName) {
        String scName = DatabaseIdentifiers.standardIdentifier(columnName);
        if (scName.equals(
                DatabaseIdentifiers.standardIdentifier(
                        sourceProp.<Column>getStorage().getName()
                )
            )
        ) {
            return sourceProp;
        }
        if (scName.equals(
                DatabaseIdentifiers.standardIdentifier(
                        targetProp.<Column>getStorage().getName()
                )
            )
        ) {
            return targetProp;
        }
        throw new IllegalArgumentException(
                "There is no property whose column name is \"" +
                        columnName +
                        "\" in type \"" +
                        this +
                        "\""
        );
    }

    @Override
    public Map<String, ImmutableProp> getSelectableProps() {
        return props;
    }

    @Override
    public Map<String, ImmutableProp> getSelectableReferenceProps() {
        return props;
    }

    @Override
    public BiFunction<DraftContext, Object, Draft> getDraftFactory() {
        throw new UnsupportedOperationException("draftFactory is not supported by AssociationType");
    }

    @Override
    public boolean isAssignableFrom(ImmutableType type) {
        return false;
    }

    @Override
    public ImmutableType getSuperType() {
        return null;
    }

    @Override
    public ImmutableProp getIdProp() {
        throw new UnsupportedOperationException("Id property is not supported by association type");
    }

    @Override
    public ImmutableProp getVersionProp() {
        return null;
    }

    @Override
    public Set<ImmutableProp> getKeyProps() {
        return Collections.emptySet();
    }

    @Override
    public IdGenerator getIdGenerator() {
        return null;
    }

    @Override
    public String toString() {
        return "MiddleTable(" + baseProp + ")";
    }
}
