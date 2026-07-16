package org.babyfish.jimmer.dto.compiler;

import org.babyfish.jimmer.dto.compiler.spi.BaseProp;
import org.babyfish.jimmer.dto.compiler.spi.BaseType;

import java.util.*;

class DtoFragmentRegistry<T extends BaseType, P extends BaseProp> {

    private final Map<String, DtoFragment<T, P>> fragmentMap = new LinkedHashMap<>();

    void add(
            CompilerContext<T, P> ctx,
            DtoParser.DtoFragmentContext ast
    ) {
        String qualifiedName = ctx.getDtoQualifiedName(ast.name.getText());
        T baseType = ctx.resolveTargetType(ast.targetType, ast.name, "fragment");
        DtoFragment<T, P> fragment = new DtoFragment<>(ctx, ast, baseType, qualifiedName);
        if (fragmentMap.put(qualifiedName, fragment) != null) {
            throw ctx.exception(
                    ast.name.getLine(),
                    ast.name.getCharPositionInLine(),
                    "Duplicated fragment name \"" + qualifiedName + "\""
            );
        }
    }

    boolean contains(String qualifiedName) {
        return fragmentMap.containsKey(qualifiedName);
    }

    DtoFragmentUse<T, P> resolve(
            CompilerContext<T, P> ctx,
            DtoParser.IncludeContext include,
            T baseType
    ) {
        String qualifiedName = ctx.resolveFragmentType(include.fragmentType);
        DtoFragment<T, P> fragment = fragmentMap.get(qualifiedName);
        if (fragment == null) {
            throw ctx.exception(
                    include.fragmentType.start.getLine(),
                    include.fragmentType.start.getCharPositionInLine(),
                    "Cannot resolve fragment \"" + qualifiedName + "\""
            );
        }
        if (!isAssignableFrom(fragment.baseType, baseType, ctx)) {
            throw ctx.exception(
                    include.fragmentType.start.getLine(),
                    include.fragmentType.start.getCharPositionInLine(),
                    "Fragment \"" +
                            qualifiedName +
                            "\" targets immutable type \"" +
                            fragment.baseType.getQualifiedName() +
                            "\" which is not the same as or a supertype of \"" +
                            baseType.getQualifiedName() +
                            "\""
            );
        }
        return new DtoFragmentUse<>(fragment, include);
    }

    void validate() {
        for (DtoFragment<T, P> fragment : fragmentMap.values()) {
            validateBody(fragment, fragment.ast.body);
        }
        Map<DtoFragment<T, P>, Integer> stateMap = new IdentityHashMap<>();
        List<DtoFragment<T, P>> stack = new ArrayList<>();
        for (DtoFragment<T, P> fragment : fragmentMap.values()) {
            validateCycles(fragment, stateMap, stack);
        }
    }

    private void validateCycles(
            DtoFragment<T, P> fragment,
            Map<DtoFragment<T, P>, Integer> stateMap,
            List<DtoFragment<T, P>> stack
    ) {
        Integer state = stateMap.get(fragment);
        if (state != null) {
            return;
        }
        stateMap.put(fragment, 1);
        stack.add(fragment);
        for (DtoParser.IncludeContext include : fragment.ast.body.includes) {
            DtoFragmentUse<T, P> use = resolve(fragment.ctx, include, fragment.baseType);
            Integer dependencyState = stateMap.get(use.fragment);
            if (dependencyState != null && dependencyState == 1) {
                int index = stack.indexOf(use.fragment);
                List<String> path = new ArrayList<>();
                for (int i = index; i < stack.size(); i++) {
                    path.add(stack.get(i).qualifiedName);
                }
                path.add(use.fragment.qualifiedName);
                throw fragment.ctx.exception(
                        include.fragmentType.start.getLine(),
                        include.fragmentType.start.getCharPositionInLine(),
                        "Fragment inclusion cycle: " + String.join(" -> ", path)
                );
            }
            if (dependencyState == null) {
                validateCycles(use.fragment, stateMap, stack);
            }
        }
        stack.remove(stack.size() - 1);
        stateMap.put(fragment, 2);
    }

    private void validateBody(
            DtoFragment<T, P> fragment,
            DtoParser.DtoBodyContext body
    ) {
        if (!body.typesBlocks.isEmpty()) {
            DtoParser.TypesBlockContext block = body.typesBlocks.get(0);
            throw fragment.ctx.exception(
                    block.start.getLine(),
                    block.start.getCharPositionInLine(),
                    "#types is not supported inside fragment \"" + fragment.qualifiedName + "\""
            );
        }
        for (DtoParser.ExplicitPropContext prop : body.explicitProps) {
            if (prop.foldProp() != null) {
                validateBody(fragment, prop.foldProp().dtoBody());
            } else if (prop.positiveProp() != null && prop.positiveProp().dtoBody() != null) {
                validateBody(fragment, prop.positiveProp().dtoBody());
            } else if (prop.aliasGroup() != null) {
                for (DtoParser.PositivePropContext child : prop.aliasGroup().props) {
                    if (child.dtoBody() != null) {
                        validateBody(fragment, child.dtoBody());
                    }
                }
            }
        }
    }

    private boolean isAssignableFrom(
            T superType,
            T type,
            CompilerContext<T, P> ctx
    ) {
        if (ctx.isSameType(superType, type)) {
            return true;
        }
        for (T directSuperType : ctx.getSuperTypes(type)) {
            if (isAssignableFrom(superType, directSuperType, ctx)) {
                return true;
            }
        }
        return false;
    }
}
