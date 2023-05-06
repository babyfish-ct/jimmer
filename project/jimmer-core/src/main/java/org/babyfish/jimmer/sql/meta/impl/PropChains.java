package org.babyfish.jimmer.sql.meta.impl;

import org.babyfish.jimmer.meta.EmbeddedLevel;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.meta.*;

import java.util.*;
import java.util.regex.Pattern;

public class PropChains {

    private static final Pattern DOT_PATTERN = Pattern.compile("\\.");

    private PropChains() {}

    public static Map<String, List<ImmutableProp>> of(ImmutableType type, MetadataStrategy strategy) {
        Map<String, List<ImmutableProp>> map = new HashMap<>();
        for (ImmutableProp prop : type.getProps().values()) {
            addInto(prop, strategy, map);
        }
        return map;
    }

    private static void addInto(ImmutableProp prop, MetadataStrategy strategy, Map<String, List<ImmutableProp>> outputMap) {
        Storage storage = prop.getStorage(strategy);
        if (prop.isEmbedded(EmbeddedLevel.BOTH)) {
            MultipleJoinColumns multipleJoinColumns = null;
            List<ImmutableProp> baseChain;
            if (prop.isReference(TargetLevel.PERSISTENT)) {
                if (storage instanceof MultipleJoinColumns) {
                    multipleJoinColumns = (MultipleJoinColumns) storage;
                }
                baseChain = new ArrayList<>();
                baseChain.add(prop);
                prop = prop.getTargetType().getIdProp();
            } else {
                baseChain = Collections.emptyList();
            }
            for (Map.Entry<String, EmbeddedColumns.Partial> e : prop.<EmbeddedColumns>getStorage(strategy).getPartialMap().entrySet()) {
                EmbeddedColumns.Partial partial = e.getValue();
                if (!partial.isEmbedded()) {
                    ImmutableProp partProp = prop;
                    String cmpName = DatabaseIdentifiers.comparableIdentifier(partial.name(0));
                    String path = e.getKey();
                    List<ImmutableProp> chain = new ArrayList<>(baseChain);
                    chain.add(partProp);
                    ImmutableType targetType = partProp.getTargetType();
                    if (path != null) {
                        for (String part : DOT_PATTERN.split(path)) {
                            partProp = targetType.getProp(part);
                            targetType = partProp.getTargetType();
                            chain.add(partProp);
                        }
                    }
                    if (multipleJoinColumns != null) {
                        int index = multipleJoinColumns.size() - 1;
                        while (index >= 0) {
                            String referencedName = multipleJoinColumns.referencedName(index);
                            if (DatabaseIdentifiers.comparableIdentifier(referencedName).equals(cmpName)) {
                                outputMap.put(multipleJoinColumns.name(index), Collections.unmodifiableList(chain));
                                break;
                            }
                            --index;
                        }
                        if (index == -1) {
                            throw new AssertionError(
                                    "Internal bug: Cannot find column name by reference columnName"
                            );
                        }
                    } else {
                        outputMap.put(cmpName, Collections.unmodifiableList(chain));
                    }
                }
            }
        } else if (storage instanceof SingleColumn) {
            String cmpName = DatabaseIdentifiers.comparableIdentifier(((SingleColumn)storage).getName());
            outputMap.put(cmpName, Collections.singletonList(prop));
        }
    }
}
