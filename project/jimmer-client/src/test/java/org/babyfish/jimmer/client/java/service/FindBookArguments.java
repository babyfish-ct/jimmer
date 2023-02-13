package org.babyfish.jimmer.client.java.service;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;

public class FindBookArguments {

    @NotNull
    private String name;

    @Nullable
    private String storeName;

    @Nullable
    private String[] authorNames;

    @Nullable
    private BigDecimal minPrice;

    @Nullable
    private BigDecimal maxPrice;

    @NotNull
    public String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    @Nullable
    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(@Nullable String storeName) {
        this.storeName = storeName;
    }

    @Nullable
    public String[] getAuthorNames() {
        return authorNames;
    }

    public void setAuthorName(@Nullable String[] authorNames) {
        this.authorNames = authorNames;
    }

    @Nullable
    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(@Nullable BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    @Nullable
    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(@Nullable BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }
}
