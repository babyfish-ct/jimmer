package org.babyfish.jimmer.impl.util;

import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.jackson.Converter;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class DtoPropAccessor {

    private final boolean acceptNull;

    private final PropId propId;

    private final PropId[] propIds;

    private final Function<Object, Object> getterConverter;

    private final Function<Object, Object> setterConverter;

    public DtoPropAccessor(boolean acceptNull, int[] propIds) {
        this(acceptNull, propIds, null, null);
    }

    public DtoPropAccessor(
            boolean acceptNull,
            int[] propIds,
            Function<Object, Object> getterConverter,
            Function<Object, Object> setterConverter
    ) {
        this.acceptNull = acceptNull;
        switch (propIds.length) {
            case 0:
                throw new IllegalArgumentException("`propIds` cannot be empty");
            case 1:
                this.propId = PropId.byIndex(propIds[0]);
                this.propIds = null;
                break;
            default:
                this.propId = null;
                PropId[] arr = new PropId[propIds.length];
                for (int i = arr.length - 1; i >= 0; --i) {
                    arr[i] = PropId.byIndex(propIds[i]);
                }
                this.propIds = arr;
                break;
        }
        this.getterConverter = getterConverter;
        this.setterConverter = setterConverter;
    }

    public <T> T get(Object immutable, String nullMessage) {
        T value = get(immutable);
        if (value == null) {
            throw new IllegalArgumentException(nullMessage);
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Object immutable) {
        ImmutableSpi spi = (ImmutableSpi) immutable;
        Function<Object, Object> mapper = getterConverter;
        PropId thePropId = propId;
        if (thePropId != null) {
            if (!spi.__isLoaded(thePropId)) {
                return null;
            }
            Object value = spi.__get(propId);
            return mapper != null && value != null ? (T) mapper.apply(value) : (T) value;
        }

        for (PropId propId : propIds) {
            if (!spi.__isLoaded(propId)) {
                return null;
            }
            Object value = spi.__get(propId);
            if (!(value instanceof ImmutableSpi)) {
                return mapper != null && value != null ? (T) mapper.apply(value) : (T) value;
            }
            spi = (ImmutableSpi) value;
        }
        return mapper != null ? (T) mapper.apply(spi) : (T) spi;
    }

    public void set(Draft draft, Object value) {
        if (draft == null) {
            throw new IllegalArgumentException("draft cannot be null");
        }
        if (value != null && setterConverter != null) {
            value = setterConverter.apply(value);
        }
        if (value == null && !acceptNull) {
            return;
        }

        DraftSpi spi = (DraftSpi) draft;
        PropId thePropId = propId;
        if (thePropId != null) {
            spi.__set(thePropId, value);
            return;
        }

        int depth = propIds.length;
        DraftSpi[] sources = new DraftSpi[depth];
        ImmutableProp[] props = new ImmutableProp[depth];
        for (int i = 0; i < depth; i++) {
            PropId propId = propIds[i];
            if (spi != null) {
                sources[i] = spi;
                props[i] = spi.__type().getProp(propId);
                if (props[i].getTargetType() != null) {
                    spi = (DraftSpi) (spi.__isLoaded(propId) ? spi.__get(propId) : null);
                } else {
                    spi = null;
                }
            } else {
                props[i] = props[i - 1].getTargetType().getProp(propId);
            }
            if (props[i].getTargetType() == null && i + 1 < depth) {
                throw notAssociation(i);
            }
        }
        if (value == null && !props[depth - 1].isNullable()) {
            return;
        }
        for (int i = depth - 1; i >= 0; --i) {
            PropId propId = propIds[i];
            Object deeperValue = value;
            DraftSpi source = sources[i];
            if (source != null) {
                source.__set(propId, value);
                break;
            } else {
                ImmutableType immutableType = i > 0 ? props[i - 1].getTargetType() : ((DraftSpi) draft).__type();
                value = Internal.produce(immutableType, null, d -> {
                    ((DraftSpi)d).__set(propId, deeperValue);
                });
            }
        }
    }

    private static RuntimeException notAssociation(int index) {
        return new IllegalArgumentException("props[" + index + "] is not association");
    }

    @SuppressWarnings("unchecked")
    public static Function<Object, Object> idListGetter(Class<?> targetEntityType, Converter<?, ?> targetIdConverter) {
        ImmutableType targetType = ImmutableType.get(targetEntityType);
        PropId targetIdPropId = targetType.getIdProp().getId();
        return arg -> {
            List<ImmutableSpi> targets = (List<ImmutableSpi>) arg;
            List<Object> targetIds = new ArrayList<>(targets.size());
            if (targetIdConverter == null) {
                for (ImmutableSpi target : targets) {
                    targetIds.add(target.__get(targetIdPropId));
                }
            } else {
                for (ImmutableSpi target : targets) {
                    targetIds.add(
                            ((Converter<Object, Object>)targetIdConverter).output(
                                    target.__get(targetIdPropId)
                            )
                    );
                }
            }
            return targetIds;
        };
    }

    @SuppressWarnings("unchecked")
    public static Function<Object, Object> idListSetter(Class<?> targetEntityType, Converter<?, ?> targetIdConverter) {
        ImmutableType targetType = ImmutableType.get(targetEntityType);
        PropId targetIdPropId = targetType.getIdProp().getId();
        return arg -> {
            List<Object> targetIds = (List<Object>) arg;
            List<Object> targets = new ArrayList<>(targetIds.size());
            if (targetIdConverter == null) {
                for (Object targetId : targetIds) {
                    targets.add(
                            Internal.produce(
                                    targetType,
                                    null,
                                    draft -> ((DraftSpi) draft).__set(targetIdPropId, targetId)
                            )
                    );
                }
            } else {
                for (Object targetId : targetIds) {
                    targets.add(
                            Internal.produce(
                                    targetType,
                                    null,
                                    draft -> ((DraftSpi) draft).__set(
                                            targetIdPropId,
                                            ((Converter<Object, Object>)targetIdConverter).input(targetId)
                                    )
                            )
                    );
                }
            }
            return targets;
        };
    }

    @SuppressWarnings("unchecked")
    public static Function<Object, Object> idReferenceGetter(Class<?> targetEntityType, Converter<?, ?> targetIdConverter) {
        ImmutableType targetType = ImmutableType.get(targetEntityType);
        PropId targetIdPropId = targetType.getIdProp().getId();
        if (targetIdConverter == null) {
            return arg -> ((ImmutableSpi) arg).__get(targetIdPropId);
        }
        return arg -> ((Converter<Object, Object>)targetIdConverter).output(
                ((ImmutableSpi) arg).__get(targetIdPropId)
        );
    }

    @SuppressWarnings("unchecked")
    public static Function<Object, Object> idReferenceSetter(Class<?> targetEntityType, Converter<?, ?> targetIdConverter) {
        ImmutableType targetType = ImmutableType.get(targetEntityType);
        PropId targetIdPropId = targetType.getIdProp().getId();
        return arg -> Internal.produce(
                targetType,
                null,
                draft -> ((DraftSpi) draft).__set(
                        targetIdPropId,
                        targetIdConverter == null ?
                                arg :
                                ((Converter<Object, Object>)targetIdConverter).input(arg)
                )
        );
    }

    @SuppressWarnings("unchecked")
    public static <E, D> Function<Object, Object> objectListGetter(Function<E, D> constructor) {
        return arg -> {
            List<E> entities = (List<E>) arg;
            List<D> dtoList = new ArrayList<>(entities.size());
            for (E entity : entities) {
                dtoList.add(constructor.apply(entity));
            }
            return dtoList;
        };
    }

    @SuppressWarnings("unchecked")
    public static <E, D> Function<Object, Object> objectListSetter(Function<D, E> toEntity) {
        return arg -> {
            List<D> dtoList = (List<D>) arg;
            List<E> entities = new ArrayList<>(dtoList.size());
            for (D dto : dtoList) {
                entities.add(toEntity.apply(dto));
            }
            return entities;
        };
    }

    @SuppressWarnings("unchecked")
    public static <E, D> Function<Object, Object> objectReferenceGetter(Function<E, D> constructor) {
        return (Function<Object, Object>) constructor;
    }

    @SuppressWarnings("unchecked")
    public static <E, D> Function<Object, Object> objectReferenceSetter(Function<D, E> toEntity) {
        return (Function<Object, Object>) toEntity;
    }
}
