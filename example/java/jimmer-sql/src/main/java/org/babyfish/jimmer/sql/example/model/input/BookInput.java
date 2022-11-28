package org.babyfish.jimmer.sql.example.model.input;

import java.math.BigDecimal;
import java.util.List;

public class BookInput {

    private Long id;

    private String name;

    private int edition;

    private BigDecimal price;

    private Long storeId;

    private List<Long> authorIds;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getEdition() {
        return edition;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Long getStoreId() {
        return storeId;
    }

    public List<Long> getAuthorIds() {
        return authorIds;
    }

    @Override
    public String toString() {
        return "BookInput{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", edition=" + edition +
                ", price=" + price +
                ", storeId=" + storeId +
                ", authorIds=" + authorIds +
                '}';
    }
}
