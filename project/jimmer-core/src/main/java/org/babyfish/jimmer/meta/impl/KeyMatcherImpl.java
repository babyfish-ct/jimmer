package org.babyfish.jimmer.meta.impl;

import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class KeyMatcherImpl implements KeyMatcher {

    private static final PropId[] EMPTY_PROP_IDS = new PropId[0];

    private static final Item[] EMPTY_MATCHED_ITEMS = new Item[0];

    private final ImmutableType type;

    private final Map<String, Set<ImmutableProp>> groupMap;

    private final Item[] items;

    private final List<ImmutableProp> allProps;

    public KeyMatcherImpl(ImmutableType type, Map<String, Set<ImmutableProp>> map) {
        if (map.isEmpty()) {
            throw new IllegalArgumentException("The argument `map` cannot be empty");
        }
        Map<Set<String>, String> reverseMap = new HashMap<>();
        List<Item> items = new ArrayList<>();
        Map<String, Set<ImmutableProp>> groupMap = new LinkedHashMap<>();
        Set<ImmutableProp> allProps = new LinkedHashSet<>();
        for (Map.Entry<String, Set<ImmutableProp>> e : map.entrySet()) {
            String group = e.getKey();
            Set<ImmutableProp> props = e.getValue();
            Set<String> propNames = new LinkedHashSet<>((props.size() * 4 + 2) / 3);
            for (ImmutableProp prop : props) {
                if (!propNames.add(prop.getName())) {
                    throw new IllegalArgumentException(
                            "Conflict key group \"" +
                                    group +
                                    "\", duplicated key property name \"" +
                                    prop.getName() +
                                    "\""
                    );
                }
            }
            String conflictGroup = reverseMap.put(propNames, group);
            if (conflictGroup != null) {
                throw new IllegalArgumentException(
                        "Conflict key group \"" +
                                conflictGroup +
                                "\" and \"" +
                                group +
                                "\", both of them are bound by property names: " +
                                propNames
                );
            }
            Set<ImmutableProp> standardProps = new LinkedHashSet<>((props.size() * 4 + 2) / 3);
            for (ImmutableProp prop : props) {
                if (prop.getDeclaringType() == type) {
                    standardProps.add(prop);
                } else if (prop.getDeclaringType().isAssignableFrom(type)) {
                    standardProps.add(type.getProp(prop.getName()));
                } else {
                    throw new IllegalArgumentException(
                            "The property \"" +
                                    prop +
                                    "\" does not belong to \"" +
                                    type +
                                    "\""
                    );
                }
            }
            groupMap.put(group, Collections.unmodifiableSet(standardProps));
            allProps.addAll(standardProps);
            PropId[] propIds = new PropId[standardProps.size()];
            int index = 0;
            for (ImmutableProp prop : standardProps) {
                propIds[index++] = prop.getId();
            }
            items.add(new Item(propIds, group));
        }
        items.sort((a, b) -> b.propIds.length - a.propIds.length);
        this.type = type;
        this.groupMap = Collections.unmodifiableMap(groupMap);
        this.items = items.toArray(EMPTY_MATCHED_ITEMS);
        this.allProps = Collections.unmodifiableList(new ArrayList<>(allProps));
    }

    @NotNull
    @Override
    public Map<String, Set<ImmutableProp>> toMap() {
        return groupMap;
    }

    @NotNull
    @Override
    public List<ImmutableProp> getAllProps() {
        return allProps;
    }
    
    @Nullable
    @Override
    public Group match(Object entity) {
        if (entity == null) {
            return null;
        }
        if (!type.getJavaClass().isAssignableFrom(entity.getClass())) {
            throw new IllegalArgumentException(
                    "The expected type is \"" +
                            type +
                            "\", but the actual type is \"" +
                            entity.getClass().getName() +
                            "\""
            );
        }
        ImmutableSpi spi = (ImmutableSpi) entity;
        for (Item item : items) {
            if (item.isLoaded(spi)) {
                return new Group(item.group, groupMap.get(item.group));
            }
        }
        return null;
    }

    @Override
    public Group match(Iterable<ImmutableProp> props) {
        Set<PropId> propIds = new HashSet<>();
        for (ImmutableProp prop : props) {
            if (!prop.getDeclaringType().isAssignableFrom(type)) {
                return null;
            }
            propIds.add(prop.getId());
        }
        for (Item item : items) {
            boolean unmatched = false;
            for (PropId propId : item.propIds) {
                if (!propIds.contains(propId)) {
                    unmatched = true;
                    break;
                }
            }
            if (!unmatched) {
                return new Group(item.group, groupMap.get(item.group));
            }
        }
        return null;
    }

    @Override
    public List<ImmutableProp> missedProps(Iterable<ImmutableProp> props) {
        List<ImmutableProp> missedProps = new ArrayList<>();
        Set<PropId> propIds = new HashSet<>();
        for (ImmutableProp prop : props) {
            if (!prop.getDeclaringType().isAssignableFrom(type)) {
                return null;
            }
            propIds.add(prop.getId());
        }
        for (Item item : items) {
            boolean mached = false;
            boolean unmatched = false;
            for (PropId propId : item.propIds) {
                if (propIds.contains(propId)) {
                    mached = true;
                } else {
                    unmatched = true;
                }
                if (mached && unmatched) {
                    break;
                }
            }
            if (mached && unmatched) {
                for (PropId itemPropId : item.propIds) {
                    if (!propIds.contains(itemPropId)) {
                        missedProps.add(type.getProp(itemPropId));
                    }
                }
            }
        }
        return missedProps;
    }

    @Override
    public int hashCode() {
        return groupMap.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        KeyMatcherImpl that = (KeyMatcherImpl) o;

        return groupMap.equals(that.groupMap);
    }

    @Override
    public String toString() {
        return "KeyGroup" + groupMap.toString();
    }

    private class Item {

        final PropId[] propIds;

        final String group;

        PropId[] targetIdPropIds;

        Item(PropId[] propIds, String group) {
            this.propIds = propIds;
            this.group = group;
        }

        public boolean isLoaded(ImmutableSpi spi) {
            for (PropId propId : propIds) {
                if (!spi.__isLoaded(propId)) {
                    return false;
                }
            }
            PropId[] targetIdPropIds = this.targetIdPropIds();
            if (targetIdPropIds.length != 0) {
                for (int i = propIds.length - 1; i >= 0; --i) {
                    PropId targetIdPropId = targetIdPropIds[i];
                    if (targetIdPropId != null) {
                        ImmutableSpi target = (ImmutableSpi) spi.__get(propIds[i]);
                        if (target != null && !target.__isLoaded(targetIdPropId)) {
                            throw new IllegalArgumentException(
                                    "Illegal entity \"" +
                                            spi +
                                            "\", its key property \"" +
                                            spi.__type().getProp(targetIdPropId) +
                                            "\" is loaded but the \"" +
                                            target.__type().getIdProp() +
                                            "\" of that associated object is not loaded"
                            );
                        }
                    }
                }
            }
            return true;
        }

        private PropId[] targetIdPropIds() {
            PropId[] arr = this.targetIdPropIds;
            if (arr == null) {
                ImmutableType type = KeyMatcherImpl.this.type;
                for (int i = propIds.length - 1; i >= 0; --i) {
                    ImmutableProp prop = type.getProp(propIds[i]);
                    if (arr == null) {
                        arr = new PropId[propIds.length];
                    }
                    if (prop.isReference(TargetLevel.PERSISTENT)) {
                        arr[i] = prop.getTargetType().getIdProp().getId();
                    }
                }
                if (arr == null) {
                    arr = EMPTY_PROP_IDS;
                }
                this.targetIdPropIds = arr;
            }
            return arr;
        }
    }
}
