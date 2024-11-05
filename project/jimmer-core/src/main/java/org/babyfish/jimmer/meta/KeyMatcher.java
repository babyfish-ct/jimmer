package org.babyfish.jimmer.meta;

import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class KeyMatcher {

    private static final Item[] EMPTY_MATCHED_ITEMS = new Item[0];

    private final ImmutableType type;

    private final Map<String, Set<ImmutableProp>> groupMap;

    private final Item[] items;

    private final List<ImmutableProp> allProps;

    public KeyMatcher(ImmutableType type, Map<String, Map<String, ImmutableProp>> map) {
        Map<Set<String>, String> reverseMap = new HashMap<>();
        List<Item> items = new ArrayList<>();
        Map<String, Set<ImmutableProp>> groupMap = new LinkedHashMap<>();
        Set<ImmutableProp> allProps = new LinkedHashSet<>();
        for (Map.Entry<String, Map<String, ImmutableProp>> e : map.entrySet()) {
            String group = e.getKey();
            Map<String, ImmutableProp> propMap = e.getValue();
            Set<String> propNames = propMap.keySet();
            Collection<ImmutableProp> props = propMap.values();
            String conflictGroup = reverseMap.put(propNames, group);
            if (conflictGroup != null) {
                throw new IllegalArgumentException(
                        "Conflict key group \"" +
                                conflictGroup +
                                "\" and \"" +
                                group +
                                "\", both of them are bound by properties: " +
                                props
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
            PropId[] targetIdPropIds = null;
            int index = 0;
            for (ImmutableProp prop : standardProps) {
                propIds[index] = prop.getId();
                if (prop.isReference(TargetLevel.PERSISTENT)) {
                    if (targetIdPropIds == null) {
                        targetIdPropIds = new PropId[propIds.length];
                    }
                    targetIdPropIds[index] = prop.getTargetType().getIdProp().getId();
                }
                index++;
            }
            items.add(new Item(propIds, targetIdPropIds, group));
        }
        items.sort((a, b) -> b.propIds.length - a.propIds.length);
        this.type = type;
        this.groupMap = Collections.unmodifiableMap(groupMap);
        this.items = items.toArray(EMPTY_MATCHED_ITEMS);
        this.allProps = Collections.unmodifiableList(new ArrayList<>(allProps));
    }

    @NotNull
    public Map<String, Set<ImmutableProp>> toMap() {
        return groupMap;
    }

    @NotNull
    public List<ImmutableProp> getAllProps() {
        return allProps;
    }
    
    @Nullable
    public Group match(Object entity) {
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
    public String toString() {
        return "KeyGroup" + groupMap.toString();
    }

    public static class Group {

        private final String name;

        private final Set<ImmutableProp> props;

        public Group(String name, Set<ImmutableProp> props) {
            this.name = name;
            this.props = props;
        }

        @NotNull
        public String getName() {
            return name;
        }

        @NotNull
        public Set<ImmutableProp> getProps() {
            return props;
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + props.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Group that = (Group) o;

            if (!name.equals(that.name)) return false;
            return props.equals(that.props);
        }

        @Override
        public String toString() {
            return "KeyMatcher.Group{" +
                    "name='" + name + '\'' +
                    ", props=" + props +
                    "}";
        }
    }

    private static class Item {

        final PropId[] propIds;

        final PropId[] targetIdPropIds;

        final String group;

        Item(PropId[] propIds, PropId[] targetIdPropIds, String group) {
            this.propIds = propIds;
            this.targetIdPropIds = targetIdPropIds;
            this.group = group;
        }

        public boolean isLoaded(ImmutableSpi spi) {
            for (PropId propId : propIds) {
                if (!spi.__isLoaded(propId)) {
                    return false;
                }
            }
            PropId[] targetIdPropIds = this.targetIdPropIds;
            if (targetIdPropIds != null) {
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
    }
}
