package org.babyfish.jimmer.sql.exception;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.babyfish.jimmer.client.ApiIgnore;
import org.babyfish.jimmer.error.CodeBasedRuntimeException;
import org.babyfish.jimmer.ClientException;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.ast.impl.TupleImplementor;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.runtime.ExportedSavePath;
import org.babyfish.jimmer.sql.runtime.MutationPath;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * The exception for save command
 */
@ClientException(
        family = "SAVE_COMMAND",
        subTypes = {
                SaveException.ReadonlyMiddleTable.class,
                SaveException.NullTarget.class,
                SaveException.CannotDissociateTarget.class,
                SaveException.NoIdGenerator.class,
                SaveException.IllegalIdGenerator.class,
                SaveException.IllegalGeneratedId.class,
                SaveException.IllegalInterceptorBehavior.class,
                SaveException.NoKeyProp.class,
                SaveException.NoVersion.class,
                SaveException.OptimisticLockError.class,
                SaveException.NeitherIdNorKey.class,
                SaveException.ReversedRemoteAssociation.class,
                SaveException.LongRemoteAssociation.class,
                SaveException.FailedRemoteValidation.class,
                SaveException.UnstructuredAssociation.class,
                SaveException.TargetIsNotTransferable.class,
                SaveException.IncompleteProperty.class,
                SaveException.NotUnique.class,
                SaveException.IllegalTargetId.class,
        }
)
public abstract class SaveException extends CodeBasedRuntimeException {

    private final ExportedSavePath exportedPath;

    private MutationPath path;

    public SaveException(@NotNull MutationPath path, String message) {
        super(
                message == null || message.isEmpty() ?
                        "Save error caused by the path: \"" + path + "\"" :
                        "Save error caused by the path: \"" + path + "\": " + message,
                null
        );
        this.path = path;
        this.exportedPath = path.export();
    }

    public SaveException(@NotNull ExportedSavePath path, String message) {
        super(message, null);
        this.exportedPath = path;
    }

    @JsonIgnore
    public abstract SaveErrorCode getSaveErrorCode();

    /**
     * The path where this exception occurs.
     */
    @NotNull
    public ExportedSavePath getExportedPath() {
        return exportedPath;
    }

    @ApiIgnore
    @NotNull
    public MutationPath getPath() {
        MutationPath sp = path;
        if (sp == null) {
            path = sp = MutationPath.of(exportedPath);
        }
        return sp;
    }

    @NotNull
    @Override
    public Map<String, Object> getFields() {
        return Collections.singletonMap("path", exportedPath);
    }

    /**
     * The association middle table is readonly
     */
    @ClientException(code = "READONLY_MIDDLE_TABLE")
    public static class ReadonlyMiddleTable extends SaveException {

        public ReadonlyMiddleTable(@NotNull MutationPath path, String message) {
            super(path, message);
        }

        public ReadonlyMiddleTable(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @JsonIgnore
        @Override
        public SaveErrorCode getSaveErrorCode() {
            return SaveErrorCode.READONLY_MIDDLE_TABLE;
        }
    }

    /**
     * The associated object cannot be null
     */
    @ClientException(code = "NULL_TARGET")
    public static class NullTarget extends SaveException {

        public NullTarget(@NotNull MutationPath path, String message) {
            super(path, message);
        }

        public NullTarget(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @JsonIgnore
        @Override
        public SaveErrorCode getSaveErrorCode() {
            return SaveErrorCode.NULL_TARGET;
        }
    }

    /**
     * Some child objects need to be dissociated by in order to save the current object,
     * however, no dissociation behavior if configured on the many-to-one/one-to-one association
     * from child object to parent object, by either annotation or runtime overriding.
     */
    @ClientException(code = "CANNOT_DISSOCIATE_TARGETS")
    public static class CannotDissociateTarget extends SaveException {

        public CannotDissociateTarget(@NotNull MutationPath path, String message) {
            super(path, message);
        }

        public CannotDissociateTarget(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @JsonIgnore
        @Override
        public SaveErrorCode getSaveErrorCode() {
            return SaveErrorCode.CANNOT_DISSOCIATE_TARGETS;
        }
    }

    @ClientException(code = "NO_ID_GENERATOR")
    public static class NoIdGenerator extends SaveException {

        public NoIdGenerator(@NotNull MutationPath path, String message) {
            super(path, message);
        }

        public NoIdGenerator(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @JsonIgnore
        @Override
        public SaveErrorCode getSaveErrorCode() {
            return SaveErrorCode.NO_ID_GENERATOR;
        }
    }

    @ClientException(code = "ILLEGAL_ID_GENERATOR")
    public static class IllegalIdGenerator extends SaveException {

        public IllegalIdGenerator(@NotNull MutationPath path, String message) {
            super(path, message);
        }

        public IllegalIdGenerator(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @JsonIgnore
        @Override
        public SaveErrorCode getSaveErrorCode() {
            return SaveErrorCode.ILLEGAL_ID_GENERATOR;
        }
    }

    @ClientException(code = "ILLEGAL_GENERATED_ID")
    public static class IllegalGeneratedId extends SaveException {

        public IllegalGeneratedId(@NotNull MutationPath path, String message) {
            super(path, message);
        }

        public IllegalGeneratedId(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @JsonIgnore
        @Override
        public SaveErrorCode getSaveErrorCode() {
            return SaveErrorCode.ILLEGAL_GENERATED_ID;
        }
    }

    @ClientException(code = "ILLEGAL_INTERCEPTOR_BEHAVIOR")
    public static class IllegalInterceptorBehavior extends SaveException {

        public IllegalInterceptorBehavior(@NotNull MutationPath path, String message) {
            super(path, message);
        }

        public IllegalInterceptorBehavior(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @JsonIgnore
        @Override
        public SaveErrorCode getSaveErrorCode() {
            return SaveErrorCode.ILLEGAL_INTERCEPTOR_BEHAVIOR;
        }
    }

    @ClientException(code = "NO_KEY_PROP")
    public static class NoKeyProp extends SaveException {

        public NoKeyProp(@NotNull MutationPath path, String message) {
            super(path, message);
        }

        public NoKeyProp(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @JsonIgnore
        @Override
        public SaveErrorCode getSaveErrorCode() {
            return SaveErrorCode.NO_KEY_PROP;
        }
    }

    @ClientException(code = "NO_VERSION")
    public static class NoVersion extends SaveException {

        public NoVersion(@NotNull MutationPath path, String message) {
            super(path, message);
        }

        public NoVersion(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @JsonIgnore
        @Override
        public SaveErrorCode getSaveErrorCode() {
            return SaveErrorCode.NO_VERSION;
        }
    }

    @ClientException(code = "OPTIMISTIC_LOCK_ERROR")
    public static class OptimisticLockError extends SaveException {

        public OptimisticLockError(@NotNull MutationPath path, String message) {
            super(path, message);
        }

        public OptimisticLockError(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @JsonIgnore
        @Override
        public SaveErrorCode getSaveErrorCode() {
            return SaveErrorCode.OPTIMISTIC_LOCK_ERROR;
        }
    }

    @ClientException(code = "NEITHER_ID_NOR_KEY")
    public static class NeitherIdNorKey extends SaveException {

        public NeitherIdNorKey(@NotNull MutationPath path, String message) {
            super(path, message);
        }

        public NeitherIdNorKey(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @JsonIgnore
        @Override
        public SaveErrorCode getSaveErrorCode() {
            return SaveErrorCode.NEITHER_ID_NOR_KEY;
        }
    }

    @ClientException(code = "REVERSED_REMOTE_ASSOCIATION")
    public static class ReversedRemoteAssociation extends SaveException {

        public ReversedRemoteAssociation(@NotNull MutationPath path, String message) {
            super(path, message);
        }

        public ReversedRemoteAssociation(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @JsonIgnore
        @Override
        public SaveErrorCode getSaveErrorCode() {
            return SaveErrorCode.REVERSED_REMOTE_ASSOCIATION;
        }
    }

    @ClientException(code = "LONG_REMOTE_ASSOCIATION")
    public static class LongRemoteAssociation extends SaveException {

        public LongRemoteAssociation(@NotNull MutationPath path, String message) {
            super(path, message);
        }

        public LongRemoteAssociation(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @JsonIgnore
        @Override
        public SaveErrorCode getSaveErrorCode() {
            return SaveErrorCode.LONG_REMOTE_ASSOCIATION;
        }
    }

    @ClientException(code = "FAILED_REMOTE_VALIDATION")
    public static class FailedRemoteValidation extends SaveException {

        public FailedRemoteValidation(@NotNull MutationPath path, String message) {
            super(path, message);
        }

        public FailedRemoteValidation(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @JsonIgnore
        @Override
        public SaveErrorCode getSaveErrorCode() {
            return SaveErrorCode.FAILED_REMOTE_VALIDATION;
        }
    }

    @ClientException(code = "UNSTRUCTURED_ASSOCIATION")
    public static class UnstructuredAssociation extends SaveException {

        public UnstructuredAssociation(@NotNull MutationPath path, String message) {
            super(path, message);
        }

        public UnstructuredAssociation(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @JsonIgnore
        @Override
        public SaveErrorCode getSaveErrorCode() {
            return SaveErrorCode.UNSTRUCTURED_ASSOCIATION;
        }
    }

    @ClientException(code = "TARGET_IS_NOT_TRANSFERABLE")
    public static class TargetIsNotTransferable extends SaveException {

        public TargetIsNotTransferable(@NotNull MutationPath path, String message) {
            super(path, message);
        }

        public TargetIsNotTransferable(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @Override
        public SaveErrorCode getSaveErrorCode() {
            return SaveErrorCode.TARGET_IS_NOT_TRANSFERABLE;
        }
    }

    @ClientException(code = "INCOMPLETE_PROPERTY")
    public static class IncompleteProperty extends SaveException {

        public IncompleteProperty(@NotNull MutationPath path, String message) {
            super(path, message);
        }

        public IncompleteProperty(@NotNull ExportedSavePath path, String message) {
            super(path, message);
        }

        @Override
        public SaveErrorCode getSaveErrorCode() {
            return SaveErrorCode.INCOMPLETE_PROPERTY;
        }
    }

    @ClientException(code = "NOT_UNIQUE")
    public static class NotUnique extends SaveException {

        private final Map<String, Object> valueMap;

        private final List<ImmutableProp> props;

        public NotUnique(
                @NotNull MutationPath path,
                String message,
                Set<ImmutableProp> props,
                Object value
        ) {
            super(path, message);
            Tuple2<Map<String, Object>, List<ImmutableProp>> data = data(props, value);
            this.valueMap = data.get_1();
            this.props = data.get_2();
        }

        public NotUnique(
                @NotNull ExportedSavePath path,
                String message,
                Set<ImmutableProp> props,
                Object value
        ) {
            super(path, message);
            Tuple2<Map<String, Object>, List<ImmutableProp>> data = data(props, value);
            this.valueMap = data.get_1();
            this.props = data.get_2();
        }

        @Override
        public SaveErrorCode getSaveErrorCode() {
            return SaveErrorCode.NOT_UNIQUE;
        }

        @ApiIgnore
        @NotNull
        public Map<String, Object> getValueMap() {
            return valueMap;
        }

        @ApiIgnore
        @SuppressWarnings("unchecked")
        public <T> T getValue(TypedProp.Single<?, T> prop) {
            return (T)getValue(prop.unwrap());
        }

        @ApiIgnore
        public Object getValue(ImmutableProp prop) {
            ImmutableType type = getPath().getType();
            if (!prop.getDeclaringType().isAssignableFrom(type)) {
                throw new IllegalArgumentException(
                        "The declaring type of \"" +
                                prop +
                                "\" is not assignable from \"" +
                                type +
                                "\""
                );
            }
            Object value = valueMap.get(prop.getName());
            if (value == null && !valueMap.containsKey(prop.getName())) {
                throw new IllegalArgumentException(
                        "Not value of \"" +
                                prop.getName() +
                                "\", it must be one of " +
                                valueMap.keySet()
                );
            }
            return value;
        }

        @ApiIgnore
        @NotNull
        public List<ImmutableProp> getProps() {
            return props;
        }

        @ApiIgnore
        public boolean isMatched(TypedProp.Single<?, ?> ... props) {
            ImmutableProp[] unwrappedProps = new ImmutableProp[props.length];
            for (int i = props.length - 1; i >= 0; --i) {
                unwrappedProps[i] = props[i].unwrap();
            }
            return isMatched(unwrappedProps);
        }

        @ApiIgnore
        public boolean isMatched(ImmutableProp ... props) {
            ImmutableType type = getPath().getType();
            Set<String> propNames = new LinkedHashSet<>((props.length * 4 + 2) / 3);
            for (ImmutableProp prop : props) {
                if (!prop.getDeclaringType().isAssignableFrom(type)) {
                    return false;
                }
                propNames.add(prop.getName());
            }
            return propNames.equals(this.valueMap.keySet());
        }

        private static Tuple2<Map<String, Object>, List<ImmutableProp>> data(
                Set<ImmutableProp> props,
                Object value
        ) {
            if (props.size() == 1) {
                ImmutableProp prop = props.iterator().next();
                return new Tuple2<>(
                        Collections.singletonMap(prop.getName(), value),
                        Collections.singletonList(prop)
                );
            }
            if (!(value instanceof TupleImplementor)) {
                throw new IllegalArgumentException(
                        "When the size of \"props\" is greater than 1, " +
                                "the value must be an tuple"
                );
            }
            TupleImplementor tuple = (TupleImplementor) value;
            if (props.size() != tuple.size()) {
                throw new IllegalArgumentException(
                        "When property count is " +
                                props.size() +
                                ", but the value count is " +
                                tuple.size()
                );
            }
            Map<String, Object> map = new LinkedHashMap<>((props.size() * 4 + 2) / 3);
            List<ImmutableProp> list = new ArrayList<>();
            int index = 0;
            for (ImmutableProp prop : props) {
                map.put(prop.getName(), tuple.get(index++));
                list.add(prop);
            }
            return new Tuple2<>(
                    Collections.unmodifiableMap(map),
                    Collections.unmodifiableList(list)
            );
        }
    }

    /**
     * The associated id that does not exists in database
     */
    @ClientException(code = "ILLEGAL_TARGET_ID")
    public static class IllegalTargetId extends SaveException {

        private final ImmutableProp prop;

        private final Collection<?> targetIds;

        public IllegalTargetId(@NotNull MutationPath path, String message, ImmutableProp prop, Collection<?> targetIds) {
            super(path, message);
            this.prop = prop;
            this.targetIds = targetIds;
        }

        public IllegalTargetId(@NotNull ExportedSavePath path, String message, ImmutableProp prop, Collection<?> targetIds) {
            super(path, message);
            this.prop = prop;
            this.targetIds = targetIds;
        }

        @JsonIgnore
        @Override
        public SaveErrorCode getSaveErrorCode() {
            return SaveErrorCode.ILLEGAL_TARGET_ID;
        }

        @ApiIgnore
        @NotNull
        public ImmutableProp getProp() {
            return prop;
        }

        @ApiIgnore
        @NotNull
        public Collection<?> getTargetIds() {
            return targetIds;
        }
    }
}
