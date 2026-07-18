package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.ApiService;
import org.babyfish.jimmer.client.meta.Schema;
import org.babyfish.jimmer.client.meta.TypeDefinition;
import org.babyfish.jimmer.client.meta.TypeName;

import java.io.IOException;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class SchemaImpl<S> extends AstNode<S> implements Schema {

    private Map<TypeName, ApiServiceImpl<S>> apiServiceMap;

    private Map<TypeName, TypeDefinitionImpl<S>> typeDefinitionMap = new TreeMap<>();

    public SchemaImpl() {
        this(null);
    }

    public SchemaImpl(Map<TypeName, ApiServiceImpl<S>> apiServiceMap) {
        super(null);
        this.apiServiceMap = apiServiceMap != null ?
                apiServiceMap instanceof NavigableMap<?, ?> ? apiServiceMap : new TreeMap<>(apiServiceMap) :
                new TreeMap<>();
    }

    public SchemaImpl(Map<TypeName, ApiServiceImpl<S>> apiServiceMap, Map<TypeName, TypeDefinitionImpl<S>> typeDefinitionMap) {
        this.apiServiceMap = apiServiceMap;
        this.typeDefinitionMap = typeDefinitionMap;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<TypeName, ApiService> getApiServiceMap() {
        return (Map<TypeName, ApiService>) (Map<?, ?>) apiServiceMap;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<TypeName, TypeDefinition> getTypeDefinitionMap() {
        return (Map<TypeName, TypeDefinition>) (Map<?, ?>) typeDefinitionMap;
    }

    public void addApiService(ApiServiceImpl<S> apiService) {
        this.apiServiceMap.put(apiService.getTypeName(), apiService);
    }

    public void addTypeDefinition(TypeDefinitionImpl<S> typeDefinition) {
        this.typeDefinitionMap.put(typeDefinition.getTypeName(), typeDefinition);
    }

    @Override
    public void accept(AstNodeVisitor<S> visitor) {
        try {
            if (!visitor.visitAstNode(this)) {
                return;
            }
            for (ApiServiceImpl<S> apiService : apiServiceMap.values()) {
                apiService.accept(visitor);
            }
            // Cannot visit type definitions because the current visitor is used create or mark definitions
        } finally {
            visitor.visitedAstNode(this);
        }
    }

    @Override
    public String toString() {
        return "SchemaImpl{" +
                "apiServiceMap=" + apiServiceMap +
                ", typeDefinitionMap=" + typeDefinitionMap +
                '}';
    }

    }
