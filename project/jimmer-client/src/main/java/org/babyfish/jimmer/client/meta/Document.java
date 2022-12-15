package org.babyfish.jimmer.client.meta;

import java.util.List;

public interface Document {

    List<Item> getItems();

    interface Item {

        String getText();

        int getDepth();
    }
}
