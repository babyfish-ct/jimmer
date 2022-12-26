package org.babyfish.jimmer.spring.repository.parser;

import java.util.List;

public class AndPredicate implements Predicate {

    private final List<Predicate> predicates;

    private AndPredicate(List<Predicate> predicates) {
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
        return new AndPredicate(predicates);
    }

    @Override
    public String toString() {
        return "AndPredicate{" +
                "predicates=" + predicates +
                '}';
    }
}
