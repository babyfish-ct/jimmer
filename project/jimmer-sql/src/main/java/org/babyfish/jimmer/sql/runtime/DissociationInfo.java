package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ManyToOne;
import org.babyfish.jimmer.sql.OneToOne;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.meta.MiddleTable;
import org.babyfish.jimmer.sql.meta.Storage;

import javax.validation.constraints.NotNull;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Objects;

public final class DissociationInfo {

    @NotNull
    private final List<ImmutableProp> props;

    @NotNull
    private final List<ImmutableProp> backProps;

    public DissociationInfo(List<ImmutableProp> props, List<ImmutableProp> backProps) {
        this.props = Objects.requireNonNull(props, "`props` cannot be null");
        this.backProps = Objects.requireNonNull(backProps, "`backProps` cannot be null");
    }

    public List<ImmutableProp> getProps() {
        return props;
    }

    public List<ImmutableProp> getBackProps() {
        return backProps;
    }

    public boolean isDirectlyDeletable(MetadataStrategy strategy) {
        boolean checkRequired = false;
        for (ImmutableProp backProp : backProps) {
            if (backProp.isReference(TargetLevel.PERSISTENT) && backProp.getDissociateAction() != DissociateAction.LAX) {
                checkRequired = true;
                break;
            }
            Storage storage = backProp.getStorage(strategy);
            if (storage instanceof MiddleTable) {
                MiddleTable middleTable = (MiddleTable) storage;
                if (!middleTable.isCascadeDeletedByTarget()) {
                    checkRequired = true;
                    break;
                }
            }
        }
        if (!checkRequired) {
            for (ImmutableProp prop : props) {
                Storage storage = prop.getStorage(strategy);
                if (storage instanceof MiddleTable) {
                    MiddleTable middleTable = (MiddleTable) storage;
                    if (!middleTable.isCascadeDeletedBySource()) {
                        checkRequired = true;
                        break;
                    }
                }
            }
        }
        return !checkRequired;
    }

    @Override
    public int hashCode() {
        return Objects.hash(props, backProps);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DissociationInfo that = (DissociationInfo) o;
        return Objects.equals(props, that.props) && Objects.equals(backProps, that.backProps);
    }

    @Override
    public String toString() {
        return "DissociationInfo{" +
                "props=" + props +
                ", backProps=" + backProps +
                '}';
    }
}
