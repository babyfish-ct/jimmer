package org.babyfish.jimmer.sql.ast.query.specification;

import org.babyfish.jimmer.Specification;
import org.babyfish.jimmer.client.ApiIgnore;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@ApiIgnore
public final class Specifications {

    private Specifications() {
    }

    @SafeVarargs
    @Nullable
    public static <E> Specification<E> and(Specification<? extends E>... specifications) {
        return allOf(Arrays.asList(specifications));
    }

    @Nullable
    public static <E> Specification<E> and(Iterable<? extends Specification<? extends E>> specifications) {
        return allOf(specifications);
    }

    @SafeVarargs
    @Nullable
    public static <E> Specification<E> allOf(Specification<? extends E>... specifications) {
        return allOf(Arrays.asList(specifications));
    }

    @Nullable
    public static <E> Specification<E> allOf(Iterable<? extends Specification<? extends E>> specifications) {
        return of(Op.AND, specifications);
    }

    @SafeVarargs
    @Nullable
    public static <E> Specification<E> or(Specification<? extends E>... specifications) {
        return anyOf(Arrays.asList(specifications));
    }

    @Nullable
    public static <E> Specification<E> or(Iterable<? extends Specification<? extends E>> specifications) {
        return anyOf(specifications);
    }

    @SafeVarargs
    @Nullable
    public static <E> Specification<E> anyOf(Specification<? extends E>... specifications) {
        return anyOf(Arrays.asList(specifications));
    }

    @Nullable
    public static <E> Specification<E> anyOf(Iterable<? extends Specification<? extends E>> specifications) {
        return of(Op.OR, specifications);
    }

    @Nullable
    public static <E> Specification<E> not(@Nullable Specification<? extends E> specification) {
        if (specification == null) {
            return null;
        }
        JSpecification<?, ?> implementor = asJSpecification(specification);
        return new NotSpecification<>(
                castEntityType(specification.entityType()),
                implementor
        );
    }

    @Nullable
    private static <E> Specification<E> of(
            Op op,
            Iterable<? extends Specification<? extends E>> specifications
    ) {
        List<JSpecification<?, ?>> implementors = new ArrayList<>();
        Class<?> entityType = null;
        for (Specification<? extends E> specification : specifications) {
            if (specification == null) {
                continue;
            }
            implementors.add(asJSpecification(specification));
            entityType = entityType == null ?
                    specification.entityType() :
                    commonEntityType(entityType, specification.entityType());
            if (entityType == null) {
                throw new IllegalArgumentException(
                        "The specifications cannot be composed because their entity types do not share " +
                                "a common inheritance hierarchy"
                );
            }
        }
        if (implementors.isEmpty()) {
            return null;
        }
        return new CompositeSpecification<>(
                castEntityType(entityType),
                op,
                implementors
        );
    }

    private static JSpecification<?, ?> asJSpecification(Specification<?> specification) {
        if (!(specification instanceof JSpecification<?, ?>)) {
            throw new IllegalArgumentException(
                    "The specification must be instance of \"" +
                            JSpecification.class.getName() +
                            "\""
            );
        }
        return (JSpecification<?, ?>) specification;
    }

    static void apply(PredicateApplier applier, JSpecification<?, ?> specification) {
        if (specification instanceof Wrapper) {
            applier.applyBody(specification);
        } else {
            applier.applyWithTypeContext(specification);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E> Class<E> castEntityType(Class<?> type) {
        return (Class<E>) type;
    }

    private static Class<?> commonEntityType(Class<?> a, Class<?> b) {
        if (isCommonEntityType(a, b)) {
            return a;
        }
        if (isCommonEntityType(b, a)) {
            return b;
        }
        for (Class<?> type : allSuperTypes(a)) {
            if (type != Object.class && isCommonEntityType(type, b)) {
                return type;
            }
        }
        return null;
    }

    private static boolean isCommonEntityType(Class<?> candidate, Class<?> type) {
        ImmutableType candidateType = ImmutableType.tryGet(candidate);
        ImmutableType otherType = ImmutableType.tryGet(type);
        return candidateType != null &&
                otherType != null &&
                candidateType.isEntity() &&
                otherType.isEntity() &&
                candidateType.isAssignableFrom(otherType);
    }

    private static Set<Class<?>> allSuperTypes(Class<?> type) {
        Set<Class<?>> types = new LinkedHashSet<>();
        ArrayDeque<Class<?>> deque = new ArrayDeque<>();
        deque.add(type);
        while (!deque.isEmpty()) {
            Class<?> current = deque.removeFirst();
            if (!types.add(current)) {
                continue;
            }
            Class<?> superclass = current.getSuperclass();
            if (superclass != null) {
                deque.addLast(superclass);
            }
            for (Class<?> interfaceType : current.getInterfaces()) {
                deque.addLast(interfaceType);
            }
        }
        return types;
    }

    private enum Op {
        AND,
        OR
    }

    private interface Wrapper {
    }

    private static class CompositeSpecification<E> implements JSpecification<E, Table<E>>, Wrapper {

        private final Class<E> entityType;

        private final Op op;

        private final List<JSpecification<?, ?>> specifications;

        private CompositeSpecification(
                Class<E> entityType,
                Op op,
                List<JSpecification<?, ?>> specifications
        ) {
            this.entityType = entityType;
            this.op = op;
            this.specifications = specifications;
        }

        @Override
        public Class<E> entityType() {
            return entityType;
        }

        @Override
        public void applyTo(SpecificationArgs<E, Table<E>> args) {
            Predicate[] predicates = new Predicate[specifications.size()];
            for (int i = 0; i < predicates.length; i++) {
                predicates[i] = capture(args, specifications.get(i));
            }
            args.where(op == Op.AND ? Predicate.and(predicates) : Predicate.or(predicates));
        }
    }

    private static class NotSpecification<E> implements JSpecification<E, Table<E>>, Wrapper {

        private final Class<E> entityType;

        private final JSpecification<?, ?> specification;

        private NotSpecification(Class<E> entityType, JSpecification<?, ?> specification) {
            this.entityType = entityType;
            this.specification = specification;
        }

        @Override
        public Class<E> entityType() {
            return entityType;
        }

        @Override
        public void applyTo(SpecificationArgs<E, Table<E>> args) {
            args.where(Predicate.not(capture(args, specification)));
        }
    }

    private static Predicate capture(SpecificationArgs<?, ?> args, JSpecification<?, ?> specification) {
        return args.getApplier().capture(() -> apply(args.getApplier(), specification));
    }
}
