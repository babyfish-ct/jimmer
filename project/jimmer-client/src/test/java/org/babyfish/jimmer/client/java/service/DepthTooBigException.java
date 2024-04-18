package org.babyfish.jimmer.client.java.service;

import org.babyfish.jimmer.error.CodeBasedException;
import org.babyfish.jimmer.ClientException;

@ClientException(code = "DEPTH_TOO_BIG")
public class DepthTooBigException extends CodeBasedException {

    private final int maxDepth;

    private final int currentDepth;

    public DepthTooBigException(int maxDepth, int currentDepth) {
        this.maxDepth = maxDepth;
        this.currentDepth = currentDepth;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public int getCurrentDepth() {
        return currentDepth;
    }
}
