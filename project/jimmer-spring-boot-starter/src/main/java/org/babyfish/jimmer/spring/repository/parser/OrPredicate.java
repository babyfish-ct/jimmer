package org.babyfish.jimmer.spring.repository.parser;

import java.util.List;

public class OrPredicate implements Predicate {

    private final List<Predicate> predicates;

    private OrPredicate(List<Predicate> predicates) {
        this.predicates = predicates;
    }

    public List<Predicate> getPredicates() {
        return predicates;
    }

    public static Predicate of(List<Predicate> predicates) {
        if (predicates.isEmpty()) {
            throw new IllegalArgumentException("predicates cannot be empty");
        }
        if (predicates.size() == 1) {
            return predicates.get(0);
        }
        return new OrPredicate(predicates);
    }

    @Override
    public String toString() {
        return "OrPredicate{" +
                "predicates=" + predicates +
                '}';
    }
}
