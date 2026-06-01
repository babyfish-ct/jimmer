package org.babyfish.jimmer.sql.meta.impl;

import org.babyfish.jimmer.impl.util.Classes;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.sql.IdView;
import org.babyfish.jimmer.sql.meta.*;

import java.util.*;
import java.util.regex.Pattern;

public class PropChains {

    private static final Pattern DOT_PATTERN = Pattern.compile("\\.");

    private PropChains() {}

    public static Map<String, List<ImmutableProp>> of(ImmutableType type, MetadataStrategy strategy) {
        if (!type.isEntity()) {
            throw new IllegalArgumentException(
                    "Cannot parse properties by column name because the declaring type \"" +
                            type +
                            "\" is not entity"
            );
        }
        Map<String, List<ImmutableProp>> map = new HashMap<>();
        for (ImmutableProp prop : type.getProps().values()) {
            if (!prop.isFormula()) {
                addInto(prop, strategy, map);
            }
        }
        return map;
    }

    private static void addInto(
            ImmutableProp prop,
            MetadataStrategy strategy,
            Map<String, List<ImmutableProp>> outputMap
    ) {
        ImmutableType declaringType = prop.getDeclaringType();
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
                                addInto(
                                        declaringType,
                                        multipleJoinColumns.name(index),
                                        Collections.unmodifiableList(chain),
                                        strategy,
                                        outputMap
                                );
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
                        addInto(
                                declaringType,
                                cmpName,
                                Collections.unmodifiableList(chain),
                                strategy,
                                outputMap
                        );
                    }
                }
            }
        } else if (storage instanceof SingleColumn) {
            String cmpName = DatabaseIdentifiers.comparableIdentifier(((SingleColumn)storage).getName());
            addInto(
                    declaringType,
                    cmpName,
                    Collections.singletonList(prop),
                    strategy,
                    outputMap
            );
        }
    }

    private static void addInto(
            ImmutableType type,
            String columnName,
            List<ImmutableProp> chain,
            MetadataStrategy strategy,
            Map<String, List<ImmutableProp>> outputMap
    ) {
        List<ImmutableProp> conflictChain = outputMap.get(columnName);
        if (conflictChain == null) {
            outputMap.put(columnName, chain);
            return;
        }
        if (isMappedIdConflict(type, columnName, conflictChain, chain, strategy)) {
            outputMap.put(columnName, chain);
            return;
        }
        if (isMappedIdConflict(type, columnName, chain, conflictChain, strategy)) {
            return;
        }
        ImmutableProp targetIdProp = null;
        if (chain.size() == 1 && conflictChain.size() == 1) {
            ImmutableProp prop = chain.get(0);
            ImmutableProp conflictProp = conflictChain.get(0);
            if (prop.isReference(TargetLevel.PERSISTENT) &&
                    Classes.matches(
                            prop.getTargetType().getIdProp().getElementClass(),
                            conflictProp.getElementClass()
                    )
            ) {
                targetIdProp = conflictProp;
            } else if (conflictProp.isReference(TargetLevel.PERSISTENT) &&
                    Classes.matches(
                            conflictProp.getTargetType().getIdProp().getElementClass(),
                            prop.getElementClass()
                    )
            ) {
                targetIdProp = prop;
            }
        }
        String message = "Illegal type \"" +
                type +
                "\", the column \"" +
                columnName +
                "\" is mapped by both \"" +
                chainPath(conflictChain) +
                "\" and \"" +
                chainPath(chain) +
                "\"";
        if (targetIdProp != null) {
            message += ", it looks like \"" +
                    targetIdProp +
                    "\" should be a property decorated by \"@" +
                    IdView.class.getName() +
                    "\"";
        }
        throw new ModelException(message);
    }

    private static boolean isMappedIdConflict(
            ImmutableType type,
            String columnName,
            List<ImmutableProp> mappedIdChain,
            List<ImmutableProp> idChain,
            MetadataStrategy strategy
    ) {
        if (mappedIdChain.isEmpty() || idChain.isEmpty()) {
            return false;
        }
        ImmutableProp mappedIdProp = mappedIdChain.get(0);
        MappedId mappedId = null;
        for (MappedId item : type.getMappedIds()) {
            if (item.getProp() == mappedIdProp) {
                mappedId = item;
                break;
            }
        }
        if (mappedId == null || idChain.get(0) != mappedId.getIdProp()) {
            return false;
        }
        List<ImmutableProp> idPath = mappedId.getIdPath();
        if (!idPath.isEmpty()) {
            if (idChain.size() < idPath.size() + 1) {
                return false;
            }
            for (int i = 0; i < idPath.size(); i++) {
                if (idChain.get(i + 1) != idPath.get(i)) {
                    return false;
                }
            }
        }
        String comparableColumnName = DatabaseIdentifiers.comparableIdentifier(columnName);
        for (MappedId.Column column : mappedId.getColumns(strategy)) {
            if (DatabaseIdentifiers.comparableIdentifier(column.getSourceName()).equals(comparableColumnName)) {
                return true;
            }
        }
        return false;
    }

    private static String chainPath(List<ImmutableProp> chain) {
        if (chain.size() == 1) {
            return chain.get(0).toString();
        }
        StringBuilder builder = new StringBuilder();
        Iterator<ImmutableProp> itr = chain.iterator();
        builder.append(itr.next().getName());
        while (itr.hasNext()) {
            builder.append('.').append(itr.next().getName());
        }
        return builder.toString();
    }
}
