package org.babyfish.jimmer.jackson;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import org.babyfish.jimmer.impl.util.StaticCache;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;

import java.util.*;

public class PropNameConverter {

    private final Map<ImmutableProp, String> fieldNameMap;

    private final Map<ImmutableProp, List<String>> aliasesMap;

    private PropNameConverter(
            Map<ImmutableProp, String> fieldNameMap,
            Map<ImmutableProp, List<String>> aliasesMap
    ) {
        this.fieldNameMap = fieldNameMap;
        this.aliasesMap = aliasesMap;
    }

    private static final StaticCache<Key, PropNameConverter> CACHE =
            new StaticCache<>(PropNameConverter::create, false);

    public static PropNameConverter of(MapperConfig<?> cfg, ImmutableType type) {
        return CACHE.get(new Key(cfg, Objects.requireNonNull(type, "`type` cannot be null")));
    }

    private static PropNameConverter create(Key key) {

        MapperConfig<?> cfg = key.config;
        PropertyNamingStrategy strategy = cfg.getPropertyNamingStrategy();
        Map<String, ImmutableProp> propMap = new HashMap<>();
        if (strategy != null && !(strategy instanceof PropertyNamingStrategies.NamingBase)) {
            throw new IllegalArgumentException(
                    "property naming strategy that does not inherit \"" +
                            PropertyNamingStrategies.NamingBase.class +
                            "\" cannot be supported by jimmer jackson module"
            );
        }
        PropertyNamingStrategies.NamingBase namingBase = (PropertyNamingStrategies.NamingBase) strategy;
        for (ImmutableProp prop : key.type.getProps().values()) {
            String jsonFieldName;
            JsonProperty jsonProperty = prop.getAnnotation(JsonProperty.class);
            if (jsonProperty != null && !jsonProperty.value().equals(JsonProperty.USE_DEFAULT_NAME)) {
                jsonFieldName = jsonProperty.value();
            } else if (namingBase != null) {
                jsonFieldName = namingBase.translate(prop.getName());
            } else {
                continue;
            }
            ImmutableProp conflictProp = propMap.put(jsonFieldName, prop);
            if (conflictProp != null) {
                throw new IllegalArgumentException(
                        "Same json field name \"" +
                                jsonFieldName +
                                "\" for both \"" +
                                prop +
                                "\" and \"" +
                                conflictProp +
                                "\""
                );
            }
        }
        Map<ImmutableProp, String> fieldNameMap;
        if (propMap.isEmpty()) {
            fieldNameMap = null;
        } else {
            fieldNameMap = new HashMap<>((propMap.size() * 4 + 2) / 3);
            for (Map.Entry<String, ImmutableProp> e : propMap.entrySet()) {
                fieldNameMap.put(e.getValue(), e.getKey());
            }
        }

        Map<String, ImmutableProp> aliasPropMap = new HashMap<>();
        for (ImmutableProp prop : key.type.getProps().values()) {
            JsonAlias jsonAlias = prop.getAnnotation(JsonAlias.class);
            if (jsonAlias != null) {
                for (String alias : jsonAlias.value()) {
                    if (!alias.isEmpty()) {
                        ImmutableProp conflictProp = aliasPropMap.put(alias, prop);
                        if (conflictProp != null) {
                            throw new IllegalArgumentException(
                                    "Same json alias \"" +
                                            alias +
                                            "\" translated by property naming strategy for both \"" +
                                            prop +
                                            "\" and \"" +
                                            conflictProp +
                                            "\""
                            );
                        }
                    }
                }
            }
        }
        Map<ImmutableProp, List<String>> aliasesMap;
        if (aliasPropMap.isEmpty()) {
            aliasesMap = null;
        } else {
            aliasesMap = new HashMap<>();
            for (Map.Entry<String, ImmutableProp> e : aliasPropMap.entrySet()) {
                aliasesMap
                        .computeIfAbsent(e.getValue(), it -> new ArrayList<>())
                        .add(e.getKey());
            }
        }
        return new PropNameConverter(fieldNameMap, aliasesMap);
    }

    public String fieldName(ImmutableProp prop) {
        if (fieldNameMap == null) {
            return prop.getName();
        }
        String fieldName = fieldNameMap.get(prop);
        return fieldName != null ? fieldName : prop.getName();
    }

    public List<String> aliases(ImmutableProp prop) {
        if (aliasesMap == null) {
            return Collections.emptyList();
        }
        List<String> aliases = aliasesMap.get(prop);
        return aliases != null ?
                Collections.unmodifiableList(aliases) :
                Collections.emptyList();
    }

    private static class Key {

        final MapperConfig<?> config;

        final ImmutableType type;

        private Key(MapperConfig<?> config, ImmutableType type) {
            this.config = config;
            this.type = type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(config, type);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key)) return false;
            Key key = (Key) o;
            return config.equals(key.config) && type.equals(key.type);
        }

        @Override
        public String toString() {
            return "Key{" +
                    "strategy=" + config +
                    ", type=" + type +
                    '}';
        }
    }
}
