package org.babyfish.jimmer.meta.impl;

import org.babyfish.jimmer.meta.EmbeddedLevel;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.JoinColumns;
import org.babyfish.jimmer.sql.meta.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class PropChains {

    private static final Pattern DOT_PATTERN = Pattern.compile("\\.");

    private PropChains() {}

    public static void addInto(ImmutableProp prop, Map<String, List<ImmutableProp>> map) {
        if (prop.isEmbedded(EmbeddedLevel.BOTH)) {
            MultipleJoinColumns multipleJoinColumns = null;
            List<ImmutableProp> baseChain;
            if (prop.isReference(TargetLevel.ENTITY)) {
                Storage storage = prop.getStorage();
                if (storage instanceof MultipleJoinColumns) {
                    multipleJoinColumns = (MultipleJoinColumns) storage;
                }
                baseChain = new ArrayList<>();
                baseChain.add(prop);
                prop = prop.getTargetType().getIdProp();
            } else {
                baseChain = Collections.emptyList();
            }
            for (Map.Entry<String, EmbeddedColumns.Partial> e :
                    prop.<EmbeddedColumns>getStorage().getPartialMap().entrySet()) {
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
                                map.put(multipleJoinColumns.name(index), Collections.unmodifiableList(chain));
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
                        map.put(cmpName, Collections.unmodifiableList(chain));
                    }
                }
            }
        } else if (prop.getStorage() instanceof SingleColumn) {
            String cmpName = DatabaseIdentifiers.comparableIdentifier(prop.<SingleColumn>getStorage().getName());
            map.put(cmpName, Collections.singletonList(prop));
        }
    }
}
