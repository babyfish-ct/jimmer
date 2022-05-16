package org.babyfish.jimmer.sql.association.meta;

import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.DraftContext;
import org.babyfish.jimmer.sql.association.Association;
import org.babyfish.jimmer.sql.meta.IdGenerator;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

public class AssociationType implements ImmutableType {



    @Override
    public Class<?> getJavaClass() {
        return Association.class;
    }

    @Override
    public ImmutableType getSuperType() {
        return null;
    }

    @Override
    public BiFunction<DraftContext, Object, Draft> getDraftFactory() {
        throw new UnsupportedOperationException("draftFactory is not supported by AssociationType");
    }

    @Override
    public Map<String, ImmutableProp> getDeclaredProps() {
        return null;
    }

    @Override
    public ImmutableProp getIdProp() {
        return null;
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
    public String getTableName() {
        return null;
    }

    @Override
    public Map<String, ImmutableProp> getProps() {
        return null;
    }

    @Override
    public ImmutableProp getProp(String name) {
        return null;
    }

    @Override
    public Map<String, ImmutableProp> getSelectableProps() {
        return null;
    }

    @Override
    public IdGenerator getIdGenerator() {
        return null;
    }
}
