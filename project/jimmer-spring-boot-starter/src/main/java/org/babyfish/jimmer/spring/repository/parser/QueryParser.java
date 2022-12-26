package org.babyfish.jimmer.spring.repository.parser;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.query.OrderMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class QueryParser {

    private final Context ctx;

    private final ImmutableType type;

    private Query.Action action = Query.Action.FIND;

    private int limit = Integer.MAX_VALUE;

    private boolean distinct;

    private Path selectedPath;

    private boolean allIgnoreCase;

    private Predicate predicate;

    private final List<Query.Order> orders = new ArrayList<>();

    QueryParser(Context ctx, ImmutableType type) {
        this.ctx = ctx;
        this.type = type;
    }

    public Query parse(Source source) {
        int orderByIndex = source.indexOf("OrderBy");
        Source beforeOrderByCourse;
        Source orderBySource;
        if (orderByIndex == -1) {
            beforeOrderByCourse = source;
            orderBySource = null;
        } else {
            beforeOrderByCourse = source.subSource(0, orderByIndex);
            orderBySource = source.subSource(orderByIndex + 7);
        }
        int byIndex = beforeOrderByCourse.indexOf("By");
        if (byIndex == -1 && orderByIndex == -1) {
            throw new IllegalArgumentException("Expect `By` or `OrderBy`");
        }
        Source actionSource;
        Source predicateSource;
        if (byIndex == -1) {
            actionSource = beforeOrderByCourse;
            predicateSource = null;
        } else {
            actionSource = beforeOrderByCourse.subSource(0, byIndex);
            predicateSource = beforeOrderByCourse.subSource(byIndex + 2);
        }
        Source selectedSource = parseAction(actionSource);
        if (orderByIndex > 0 && action != Query.Action.FIND) {
            throw new IllegalArgumentException("Illegal method name \"" + source.subSource(orderByIndex) + "\"");
        }
        if (!selectedSource.isEmpty()) {
            if (action != Query.Action.FIND) {
                throw new IllegalArgumentException("Illegal method name \"" + selectedSource + "\"");
            }
            List<Source> selectedSources = parseLimit(selectedSource);
            selectedSource = parseDistinct(selectedSources);
            if (selectedSource != null) {
                selectedPath = new PathParser(ctx, distinct).parse(selectedSource, type);
            }
        }
        if (predicateSource != null) {
            parsePredicates(predicateSource);
        }
        if (orderBySource != null) {
            parseOrders(orderBySource);
        }
        return new Query(action, limit, distinct, selectedPath, predicate, Collections.unmodifiableList(orders));
    }

    private Source parseAction(Source source) {
        Source restSource = source.trimStart("find", "read", "get", "query", "search");
        if (restSource != null) {
            return restSource;
        }
        restSource = source.trimStart("stream");
        if (restSource != null) {
            throw new IllegalArgumentException(
                    "method prefix \"stream\" is not supported temporarily"
            );
        }
        restSource = source.trimStart("exists");
        if (restSource != null) {
            action = Query.Action.EXISTS;
            return restSource;
        }
        restSource = source.trimStart("count");
        if (restSource != null) {
            action = Query.Action.COUNT;
            return restSource;
        }
        restSource = source.trimStart("delete");
        if (restSource != null) {
            action = Query.Action.DELETE;
            return restSource;
        }
        throw new IllegalArgumentException(
                "Illegal method prefix \"" + source + "\""
        );
    }

    private List<Source> parseLimit(Source source) {
        int topStartIndex = -1;
        int numStarIndex = -1;
        int firstIndex = source.indexOf("First");
        if (firstIndex != -1) {
            topStartIndex = firstIndex;
            numStarIndex = firstIndex + 5;
        }
        if (numStarIndex == -1) {
            int topIndex = source.indexOf("Top");
            if (topIndex != -1) {
                topStartIndex = topIndex;
                numStarIndex = topIndex + 3;
            }
        }
        if (numStarIndex == -1) {
            return source.isEmpty() ? Collections.emptyList() : Collections.singletonList(source);
        }
        int len = source.length();
        Source numSource = null;
        if (numStarIndex < len) {
            for (int i = numStarIndex; i < len; i++) {
                if (!Character.isDigit(source.charAt(i))) {
                    numSource = source.subSource(numStarIndex, i);
                    break;
                }
            }
            if (numSource == null) {
                numSource = source.subSource(numStarIndex);
            }
        }
        if (numSource == null) {
            throw new IllegalArgumentException(
                    "Cannot parse limit from \"" +
                            source +
                            "\""
            );
        }
        limit = Integer.parseInt(numSource.asString());
        Source before = source.subSource(0, topStartIndex);
        Source after = source.subSource(numStarIndex + numSource.length());
        List<Source> restSources = new ArrayList<>();
        if (!before.isEmpty()) {
            restSources.add(before);
        }
        if (!after.isEmpty()) {
            restSources.add(after);
        }
        return restSources;
    }

    private Source parseDistinct(List<Source> sources) {
        if (sources.isEmpty()) {
            return null;
        }
        if (sources.size() == 1) {
            Source restSource = sources.get(0).trimStart("Distinct");
            if (restSource != null) {
                distinct = true;
                return restSource;
            }
            restSource = sources.get(0).trimEnd("Distinct");
            if (restSource != null) {
                distinct = true;
                return restSource;
            }
        }
        if (sources.size() == 2) {
            if (sources.get(0).asString().equals("Distinct")) {
                distinct = true;
                return sources.get(1);
            }
            if (sources.get(1).asString().equals("Distinct")) {
                distinct = true;
                return sources.get(0);
            }
            throw new IllegalArgumentException("Illegal method name " + sources.get(1));
        }
        return sources.get(0);
    }

    private void parsePredicates(Source source) {
        if (source.isEmpty()) {
            return;
        }
        Source restSource = source.trimEnd("AllIgnoringCase", "AllIgnoreCase");
        if (restSource != null) {
            allIgnoreCase = true;
            predicate = parseOrPredicate(restSource);
        } else {
            predicate = parseOrPredicate(source);
        }
    }

    private Predicate parseOrPredicate(Source source) {
        List<Source> subSources = new ArrayList<>();
        while (!source.isEmpty()) {
            int orIndex = source.indexOf("Or");
            if (orIndex > 0 && orIndex + 2 < source.length()) {
                subSources.add(source.subSource(0, orIndex));
                source = source.subSource(orIndex + 2);
            } else {
                subSources.add(source);
                source = source.subSource(source.length());
            }
        }
        return OrPredicate.of(
                subSources
                        .stream()
                        .map(this::parseAndPredicate)
                        .collect(Collectors.toList())
        );
    }

    private Predicate parseAndPredicate(Source source) {
        List<Source> subSources = new ArrayList<>();
        while (!source.isEmpty()) {
            int andIndex = source.indexOf("And");
            if (andIndex > 0 && andIndex + 3 < source.length()) {
                subSources.add(source.subSource(0, andIndex));
                source = source.subSource(andIndex + 3);
            } else {
                subSources.add(source);
                source = source.subSource(source.length());
            }
        }
        return AndPredicate.of(
                subSources
                        .stream()
                        .map(this::parsePropPredicate)
                        .collect(Collectors.toList())
        );
    }

    private PropPredicate parsePropPredicate(Source source) {
        if (source.isEmpty()) {
            throw new IllegalArgumentException("Cannot parse predicate from \"" + source + "\"");
        }
        return new PropPredicateParser(ctx, distinct, allIgnoreCase).parse(source, type);
    }

    private void parseOrders(Source source) {
        while (!source.isEmpty()) {
            int ascIndex = source.indexOf("Asc");
            int descIndex = source.indexOf("Desc");
            if (ascIndex == -1 && descIndex == -1) {
                Path path = new PathParser(ctx, distinct).parse(source, type);
                if (!path.isScalar()) {
                    throw new IllegalArgumentException("The ordered property of \"" + source + "\" must be scalar");
                }
                orders.add(new Query.Order(path, OrderMode.ASC));
                break;
            }
            if (descIndex == -1 || (ascIndex > 0 && ascIndex < descIndex)) {
                Source propSource = source.subSource(0, ascIndex);
                Path path = new PathParser(ctx, distinct).parse(propSource, type);
                if (!path.isScalar()) {
                    throw new IllegalArgumentException("The ordered property of \"" + source + "\" must be scalar");
                }
                orders.add(new Query.Order(path, OrderMode.ASC));
                source = source.subSource(ascIndex + 3);
                continue;
            }
            if (ascIndex == -1 || (descIndex > 0 && descIndex < ascIndex)) {
                Source propSource = source.subSource(0, descIndex);
                Path path = new PathParser(ctx, distinct).parse(propSource, type);
                if (!path.isScalar()) {
                    throw new IllegalArgumentException("The ordered property of \"" + source + "\" must be scalar");
                }
                orders.add(new Query.Order(path, OrderMode.DESC));
                source = source.subSource(descIndex + 4);
            }
        }
    }
}
