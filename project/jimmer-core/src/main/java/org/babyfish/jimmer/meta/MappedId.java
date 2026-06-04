package org.babyfish.jimmer.meta;

import org.babyfish.jimmer.sql.MapsId;
import org.babyfish.jimmer.sql.meta.ColumnDefinition;
import org.babyfish.jimmer.sql.meta.EmbeddedColumns;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.meta.MultipleJoinColumns;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Pattern;

public final class MappedId {

    private static final Pattern DOT_PATTERN = Pattern.compile("\\.");

    private final ImmutableProp prop;

    private final ImmutableProp idProp;

    private final List<ImmutableProp> idPath;

    private MappedId(ImmutableProp prop, ImmutableProp idProp, List<ImmutableProp> idPath) {
        this.prop = prop;
        this.idProp = idProp;
        this.idPath = idPath;
    }

    @NotNull
    public ImmutableProp getProp() {
        return prop;
    }

    @NotNull
    public ImmutableProp getIdProp() {
        return idProp;
    }

    @NotNull
    public ImmutableProp getTargetIdProp() {
        return prop.getTargetType().getIdProp();
    }

    @NotNull
    public List<ImmutableProp> getIdPath() {
        return idPath;
    }

    public boolean isFull() {
        return idPath.isEmpty();
    }

    @NotNull
    public List<Column> getColumns(MetadataStrategy strategy) {
        ImmutableProp idProp = getIdProp();
        ColumnDefinition sourceDefinition = sourceDefinition(strategy);
        ColumnDefinition joinDefinition = prop.getStorage(strategy);
        ColumnDefinition targetDefinition = getTargetIdProp().getStorage(strategy);
        int size = joinDefinition.size();
        if (sourceDefinition.size() != size) {
            throw illegal(
                    "the mapped id path \"" +
                            pathText() +
                            "\" has " +
                            sourceDefinition.size() +
                            " column(s), but the join columns have " +
                            size +
                            " column(s)"
            );
        }
        if (targetDefinition.size() != size) {
            throw illegal(
                    "the target id property \"" +
                            getTargetIdProp() +
                            "\" has " +
                            targetDefinition.size() +
                            " column(s), but the join columns have " +
                            size +
                            " column(s)"
            );
        }
        List<Column> columns = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String targetColumnName = referencedColumnName(joinDefinition, i, targetDefinition);
            int targetIndex = targetDefinition.indexByComparableIdentifier(targetColumnName);
            if (targetIndex == -1) {
                throw illegal(
                        "the referenced column \"" +
                                targetColumnName +
                                "\" is not a column of the target id property \"" +
                                getTargetIdProp() +
                                "\""
                );
            }
            String sourceColumnName = joinDefinition.name(i);
            int sourceIndex = sourceDefinition.indexByComparableIdentifier(sourceColumnName);
            if (sourceIndex == -1) {
                throw illegal(
                        "the join column \"" +
                                sourceColumnName +
                                "\" is not a column of the mapped id path \"" +
                                pathText() +
                                "\""
                );
            }
            columns.add(
                    new Column(
                            sourceColumnName,
                            targetColumnName,
                            idProp.getStorage(strategy),
                            targetDefinition,
                            sourceIndex,
                            targetIndex
                    )
            );
        }
        return Collections.unmodifiableList(columns);
    }

    private ColumnDefinition sourceDefinition(MetadataStrategy strategy) {
        ColumnDefinition idDefinition = getIdProp().getStorage(strategy);
        if (idPath.isEmpty()) {
            return idDefinition;
        }
        if (!(idDefinition instanceof EmbeddedColumns)) {
            throw illegal(
                    "the non-empty value \"" +
                            pathText() +
                            "\" requires the id property \"" +
                            getIdProp() +
                            "\" to be embedded"
            );
        }
        return ((EmbeddedColumns) idDefinition).partial(pathText());
    }

    private String pathText() {
        if (idPath.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (ImmutableProp prop : idPath) {
            if (builder.length() != 0) {
                builder.append('.');
            }
            builder.append(prop.getName());
        }
        return builder.toString();
    }

    private String referencedColumnName(
            ColumnDefinition joinDefinition,
            int index,
            ColumnDefinition targetDefinition
    ) {
        if (joinDefinition instanceof MultipleJoinColumns) {
            return ((MultipleJoinColumns) joinDefinition).referencedName(index);
        }
        return targetDefinition.name(index);
    }

    private ModelException illegal(String reason) {
        return new ModelException(
                "Illegal property \"" +
                        prop +
                        "\", it cannot be decorated by @" +
                        MapsId.class.getName() +
                        " because " +
                        reason
        );
    }

    public static List<MappedId> resolve(ImmutableType type) {
        List<MappedId> mappedIds = new ArrayList<>();
        for (ImmutableProp prop : type.getProps().values()) {
            MapsId mapsId = prop.getAnnotation(MapsId.class);
            if (mapsId != null) {
                mappedIds.add(of(type, prop, mapsId));
            }
        }
        if (mappedIds.isEmpty()) {
            return Collections.emptyList();
        }
        validateNoPathOverlap(mappedIds);
        return Collections.unmodifiableList(mappedIds);
    }

    private static MappedId of(ImmutableType ownerType, ImmutableProp prop, MapsId mapsId) {
        validateAssociation(prop);
        ImmutableProp idProp = ownerType.getIdProp();
        if (idProp == null) {
            throw illegal(prop, "the owner type has no id property");
        }
        ImmutableProp targetIdProp = prop.getTargetType().getIdProp();
        if (targetIdProp == null) {
            throw illegal(prop, "the target type has no id property");
        }
        List<ImmutableProp> path = resolvePath(idProp, mapsId.value());
        Class<?> sourceType = path.isEmpty() ?
                idProp.getReturnClass() :
                path.get(path.size() - 1).getReturnClass();
        if (sourceType != targetIdProp.getReturnClass()) {
            throw illegal(
                    prop,
                    "the type of the mapped id path \"" +
                            mapsId.value() +
                            "\" is \"" +
                            sourceType.getName() +
                            "\", but the target id type is \"" +
                            targetIdProp.getReturnClass().getName() +
                            "\""
            );
        }
        if (idProp.getAnnotation(org.babyfish.jimmer.sql.GeneratedValue.class) != null) {
            throw illegal(prop, "the declaring id property \"" + idProp + "\" is generated");
        }
        return new MappedId(prop, idProp, path);
    }

    private static void validateAssociation(ImmutableProp prop) {
        if (!prop.isReference(TargetLevel.ENTITY)) {
            throw illegal(prop, "it is not a reference property");
        }
        Class<?> associationType = prop.getAssociationAnnotation().annotationType();
        if (associationType != org.babyfish.jimmer.sql.OneToOne.class &&
                associationType != org.babyfish.jimmer.sql.ManyToOne.class) {
            throw illegal(prop, "it is neither one-to-one nor many-to-one");
        }
        if (prop.getMappedBy() != null) {
            throw illegal(prop, "it is not an owning association");
        }
        if (!prop.isColumnDefinition()) {
            throw illegal(prop, "it is not mapped by join columns");
        }
        if (prop.isRemote()) {
            throw illegal(prop, "it is remote");
        }
    }

    private static List<ImmutableProp> resolvePath(ImmutableProp idProp, String pathText) {
        if (pathText.isEmpty()) {
            return Collections.emptyList();
        }
        if (!idProp.isEmbedded(EmbeddedLevel.SCALAR)) {
            throw illegal(
                    idProp,
                    "the non-empty maps-id path \"" +
                            pathText +
                            "\" requires embedded id"
            );
        }
        ImmutableType type = idProp.getTargetType();
        List<ImmutableProp> path = new ArrayList<>();
        String[] names = DOT_PATTERN.split(pathText);
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            ImmutableProp prop = type.getProps().get(name);
            if (prop == null) {
                throw illegal(
                        idProp,
                        "the maps-id path \"" +
                                pathText +
                                "\" cannot be resolved because there is no property \"" +
                                name +
                                "\" in \"" +
                                type +
                                "\""
                );
            }
            path.add(prop);
            type = prop.getTargetType();
            if (type == null && i + 1 < names.length) {
                throw illegal(
                        idProp,
                        "the maps-id path \"" +
                                pathText +
                                "\" cannot be resolved through the scalar property \"" +
                                prop +
                                "\""
                );
            }
        }
        return Collections.unmodifiableList(path);
    }

    private static void validateNoPathOverlap(List<MappedId> mappedIds) {
        for (int i = 0; i < mappedIds.size(); i++) {
            for (int ii = i + 1; ii < mappedIds.size(); ii++) {
                MappedId a = mappedIds.get(i);
                MappedId b = mappedIds.get(ii);
                if (isPrefix(a.idPath, b.idPath) || isPrefix(b.idPath, a.idPath)) {
                    throw illegal(
                            b.prop,
                            "its mapped id path overlaps with the path of \"" +
                                    a.prop +
                                    "\""
                    );
                }
            }
        }
    }

    private static boolean isPrefix(List<ImmutableProp> a, List<ImmutableProp> b) {
        if (a.size() > b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i) != b.get(i)) {
                return false;
            }
        }
        return true;
    }

    private static ModelException illegal(ImmutableProp prop, String reason) {
        return new ModelException(
                "Illegal property \"" +
                        prop +
                        "\", it cannot be decorated by @" +
                        MapsId.class.getName() +
                        " because " +
                        reason
        );
    }

    public static final class Column {

        private final String sourceName;

        private final String targetName;

        private final ColumnDefinition idDefinition;

        private final ColumnDefinition targetIdDefinition;

        private final int sourceIndex;

        private final int targetIndex;

        private Column(
                String sourceName,
                String targetName,
                ColumnDefinition idDefinition,
                ColumnDefinition targetIdDefinition,
                int sourceIndex,
                int targetIndex
        ) {
            this.sourceName = sourceName;
            this.targetName = targetName;
            this.idDefinition = idDefinition;
            this.targetIdDefinition = targetIdDefinition;
            this.sourceIndex = sourceIndex;
            this.targetIndex = targetIndex;
        }

        public String getSourceName() {
            return sourceName;
        }

        public String getTargetName() {
            return targetName;
        }

        public ColumnDefinition getIdDefinition() {
            return idDefinition;
        }

        public ColumnDefinition getTargetIdDefinition() {
            return targetIdDefinition;
        }

        public int getSourceIndex() {
            return sourceIndex;
        }

        public int getTargetIndex() {
            return targetIndex;
        }
    }
}
